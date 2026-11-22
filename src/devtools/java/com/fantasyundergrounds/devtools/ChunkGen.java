package com.fantasyundergrounds.devtools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/**
 * Generates a rectangle of chunks to a target status in one parallel batch, blocking the server
 * thread until every chunk is done. Because the server thread does not tick while we block, no
 * scheduled fluid/block ticks run and no tickets expire between generation and scanning, so
 * the scanned state is exactly what world generation produced.
 *
 * Uses ServerChunkCache#getChunkFutureMainThread (private) via reflection: unlike the public
 * getChunkFuture it does not block per chunk, so the worker pool generates the whole batch
 * concurrently. Minecraft 26.x is unobfuscated, so the name is stable.
 */
final class ChunkGen {
    private static final Method GET_FUTURE_MAIN;

    static {
        try {
            GET_FUTURE_MAIN = ServerChunkCache.class.getDeclaredMethod(
                "getChunkFutureMainThread", int.class, int.class, ChunkStatus.class, boolean.class);
            GET_FUTURE_MAIN.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private ChunkGen() {}

    record Result(long nanos, List<ChunkAccess> chunks) {}

    /** @return wall-clock nanoseconds spent generating to FULL; throws if any chunk failed. */
    static long generate(ServerChunkCache cache, int minX, int minZ, int maxXExcl, int maxZExcl) {
        return generate(cache, minX, minZ, maxXExcl, maxZExcl, ChunkStatus.FULL).nanos();
    }

    @SuppressWarnings("unchecked")
    static Result generate(ServerChunkCache cache, int minX, int minZ, int maxXExcl, int maxZExcl, ChunkStatus status) {
        long t0 = System.nanoTime();
        List<CompletableFuture<ChunkResult<ChunkAccess>>> futures = new ArrayList<>();
        try {
            for (int x = minX; x < maxXExcl; x++) {
                for (int z = minZ; z < maxZExcl; z++) {
                    futures.add((CompletableFuture<ChunkResult<ChunkAccess>>) GET_FUTURE_MAIN.invoke(cache, x, z, status, true));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        while (!all.isDone()) {
            if (!cache.pollTask()) {
                LockSupport.parkNanos(100_000L);
            }
        }
        long dt = System.nanoTime() - t0;
        List<ChunkAccess> chunks = new ArrayList<>(futures.size());
        for (CompletableFuture<ChunkResult<ChunkAccess>> f : futures) {
            ChunkResult<ChunkAccess> r = f.join();
            if (!r.isSuccess()) {
                throw new IllegalStateException("chunk generation failed: " + r.getError());
            }
            chunks.add(r.orElse(null));
        }
        return new Result(dt, chunks);
    }
}
