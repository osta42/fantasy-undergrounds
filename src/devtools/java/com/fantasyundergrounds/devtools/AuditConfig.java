package com.fantasyundergrounds.devtools;

import java.util.Properties;

/**
 * Parsed fu-audit.properties.
 *
 * @param label       name of this run, used for the output file name
 * @param centerX     region centre, chunk X
 * @param centerZ     region centre, chunk Z
 * @param radius      region half-size in chunks; region is (2*radius)^2 chunks
 * @param tile        tile edge length in chunks; each tile is generated in one parallel batch
 * @param scan        run the per-block scan (histogram, ores, hashes, spawnable proxy)
 * @param structures  run the structure probe (gate 6)
 * @param structureSearchRadius search radius for /locate-equivalent, in chunks
 * @param registryDump write the biome feature/carver dump (gate 7)
 * @param estimateRadius    half-size in blocks of the density-only open-volume estimate; 0 = off
 * @param estimateOnly      run only the estimate, skip chunk generation
 * @param terrainHashRadius half-size in chunks of a separate region generated only up to the
 *                    carver stage (terrain without decoration) and hashed for gate 5a; 0 = off
 */
public record AuditConfig(
    String label,
    int centerX,
    int centerZ,
    int radius,
    int tile,
    boolean scan,
    boolean structures,
    int structureSearchRadius,
    boolean registryDump,
    int terrainHashRadius,
    int estimateRadius,
    boolean estimateOnly
) {
    static AuditConfig from(Properties p) {
        return new AuditConfig(
            p.getProperty("label", "unnamed"),
            Integer.parseInt(p.getProperty("centerChunkX", "512")),
            Integer.parseInt(p.getProperty("centerChunkZ", "512")),
            Integer.parseInt(p.getProperty("radiusChunks", "16")),
            Integer.parseInt(p.getProperty("tileChunks", "16")),
            Boolean.parseBoolean(p.getProperty("scan", "true")),
            Boolean.parseBoolean(p.getProperty("structures", "true")),
            Integer.parseInt(p.getProperty("structureSearchRadius", "100")),
            Boolean.parseBoolean(p.getProperty("registryDump", "true")),
            Integer.parseInt(p.getProperty("terrainHashRadius", "8")),
            Integer.parseInt(p.getProperty("estimateRadius", "0")),
            Boolean.parseBoolean(p.getProperty("estimateOnly", "false"))
        );
    }
}
