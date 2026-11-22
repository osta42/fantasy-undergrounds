package com.fantasyundergrounds.devtools;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.ModList;

/**
 * Drives one audit run from the server tick loop. Each tick processes one tile: generate it
 * (blocking, in parallel on the worldgen pool), scan it, then return so the server can tick,
 * let the temporary tickets expire and unload/save chunks before the next tile.
 */
final class AuditRunner {
    private static final int WARMUP_TICKS = 40;

    private final MinecraftServer server;
    private final AuditConfig config;
    private final ChunkScanner scanner = new ChunkScanner();
    private final List<int[]> tiles = new ArrayList<>();
    private final List<Double> tileMsPerChunk = new ArrayList<>();
    private int tickCount;
    private int nextTile;
    private long genNanos;
    private long genChunks;
    private boolean finished;
    private boolean warmedUp;

    AuditRunner(MinecraftServer server, AuditConfig config) {
        this.server = server;
        this.config = config;
        int minX = config.centerX() - config.radius();
        int minZ = config.centerZ() - config.radius();
        int maxX = config.centerX() + config.radius();
        int maxZ = config.centerZ() + config.radius();
        for (int x = minX; x < maxX; x += config.tile()) {
            for (int z = minZ; z < maxZ; z += config.tile()) {
                this.tiles.add(new int[] {x, z, Math.min(x + config.tile(), maxX), Math.min(z + config.tile(), maxZ)});
            }
        }
    }

    AuditConfig config() {
        return this.config;
    }

    void tick() {
        if (this.finished || ++this.tickCount < WARMUP_TICKS) {
            return;
        }
        try {
            if (!this.warmedUp) {
                // JIT warm-up on a disjoint area (not measured, not scanned): without it the first
                // measured tiles run ~1.7x slower and skew the performance gate.
                this.warmedUp = true;
                int wx = this.config.centerX() - this.config.radius() - 64;
                long dt = ChunkGen.generate(this.server.overworld().getChunkSource(), wx, this.config.centerZ() - 8, wx + 16, this.config.centerZ() + 8);
                FuDevTools.LOGGER.info("[fu-audit] warm-up tile done in {} ms", dt / 1_000_000);
                return;
            }
            if (this.nextTile < this.tiles.size()) {
                processTile(this.tiles.get(this.nextTile++));
                return;
            }
            finish();
        } catch (RuntimeException e) {
            FuDevTools.LOGGER.error("[fu-audit] FAILED", e);
            this.finished = true;
            this.server.halt(false);
        }
    }

    private void processTile(int[] t) {
        ServerLevel level = this.server.overworld();
        long dt = ChunkGen.generate(level.getChunkSource(), t[0], t[1], t[2], t[3]);
        int n = (t[2] - t[0]) * (t[3] - t[1]);
        this.genNanos += dt;
        this.genChunks += n;
        this.tileMsPerChunk.add(dt / 1_000_000.0 / n);
        if (this.config.scan()) {
            for (int x = t[0]; x < t[2]; x++) {
                for (int z = t[1]; z < t[3]; z++) {
                    LevelChunk chunk = level.getChunkSource().getChunkNow(x, z);
                    if (chunk == null) {
                        throw new IllegalStateException("chunk " + x + "," + z + " not FULL after generation");
                    }
                    this.scanner.scan(level, chunk);
                }
            }
        }
        FuDevTools.LOGGER.info("[fu-audit] tile {}/{} done: {} chunks in {} ms ({} ms/chunk)",
            this.nextTile, this.tiles.size(), n, dt / 1_000_000, String.format("%.2f", dt / 1_000_000.0 / n));
    }

    private void finish() {
        this.finished = true;
        ServerLevel level = this.server.overworld();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("label", this.config.label());
        out.put("config", this.config);
        out.put("seed", level.getSeed());
        List<String> mods = new ArrayList<>();
        ModList.get().getMods().forEach(m -> mods.add(m.getModId() + "@" + m.getVersion()));
        out.put("mods", mods);
        out.put("availableProcessors", Runtime.getRuntime().availableProcessors());

        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("chunks", this.genChunks);
        perf.put("totalGenMs", this.genNanos / 1_000_000.0);
        perf.put("msPerChunk", this.genNanos / 1_000_000.0 / Math.max(1, this.genChunks));
        perf.put("tileMsPerChunk", this.tileMsPerChunk);
        out.put("performance", perf);

        if (this.config.scan()) {
            Map<String, Object> scan = new LinkedHashMap<>();
            scan.put("chunks", this.scanner.chunks);
            scan.put("blocksBelowZero", this.scanner.blocksBelowZero);
            scan.put("airBelowZero", this.scanner.airBelowZero);
            scan.put("airBelowZeroPct", 100.0 * this.scanner.airBelowZero / Math.max(1, this.scanner.blocksBelowZero));
            scan.put("waterSourcesBelowZero", this.scanner.waterBelowZero);
            scan.put("lavaSourcesBelowZero", this.scanner.lavaBelowZero);
            scan.put("darkSpawnableFloor", this.scanner.darkSpawnableFloor);
            scan.put("scheduledFluidTicks", this.scanner.scheduledFluidTicks);
            scan.put("scheduledBlockTicks", this.scanner.scheduledBlockTicks);
            scan.put("ores", this.scanner.oresById());
            scan.put("histogram", this.scanner.histogramById());
            scan.put("chunkHashes", this.scanner.chunkHashes);
            scan.put("sectionHashes", this.scanner.sectionHashes);
            out.put("scan", scan);
        }
        if (this.config.terrainHashRadius() > 0) {
            // Gate 5a: terrain-only determinism. A disjoint region is generated only to CARVERS
            // (noise, surface, carvers - no features), where vanilla is expected to be exact.
            int r = this.config.terrainHashRadius();
            int cx = this.config.centerX() + this.config.radius() + 64;
            int cz = this.config.centerZ();
            ChunkGen.Result res = ChunkGen.generate(level.getChunkSource(), cx - r, cz - r, cx + r, cz + r,
                net.minecraft.world.level.chunk.status.ChunkStatus.CARVERS);
            Map<String, String> terrain = new java.util.TreeMap<>();
            for (net.minecraft.world.level.chunk.ChunkAccess c : res.chunks()) {
                terrain.put(c.getPos().x() + "," + c.getPos().z(), ChunkScanner.hash(c));
            }
            out.put("terrainHashes", terrain);
        }
        if (this.config.structures()) {
            BlockPos origin = new BlockPos(this.config.centerX() * 16 + 8, 0, this.config.centerZ() * 16 + 8);
            out.put("structures", StructureProbe.run(level, origin, this.config.structureSearchRadius()));
        }
        if (this.config.registryDump()) {
            out.put("registry", RegistryDump.run(this.server));
        }

        Path file = this.server.getServerDirectory().resolve("fu-audit").resolve(this.config.label() + ".json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                new GsonBuilder().setPrettyPrinting().serializeSpecialFloatingPointValues().create().toJson(out, w);
            }
            FuDevTools.LOGGER.info("[fu-audit] COMPLETE -> {}", file.toAbsolutePath());
        } catch (IOException e) {
            FuDevTools.LOGGER.error("[fu-audit] cannot write {}", file, e);
        }
        this.server.halt(false);
    }
}
