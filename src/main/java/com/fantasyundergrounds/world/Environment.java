package com.fantasyundergrounds.world;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Underground environments. Zones, not biomes (PLAN.md 2.1). */
public enum Environment implements StringRepresentable {
    /** No cavern; ordinary tunnel networks only. */
    ORDINARY("ordinary"),
    GRAND_CAVERNS("grand_caverns"),
    DROWNED_ABYSSES("drowned_abysses"),
    INFERNAL_CHASMS("infernal_chasms"),
    UNDERGROUND_FORESTS("underground_forests"),
    CRYSTAL_HOLLOWS("crystal_hollows"),
    ROOT_SYSTEMS("ancient_root_systems"),
    /** Selected by its own 2D field (Phase 3), not by zone cells. */
    GRAND_CANYONS("grand_canyons"),
    /** Only inside tall Grand Cavern cells (Phase 3). */
    FLOATING_ISLANDS("floating_islands"),
    /** Structures (Phase 5). */
    ANCIENT_RUINS("ancient_ruins");

    public static final Codec<Environment> CODEC = StringRepresentable.fromEnum(Environment::values);

    private final String name;

    Environment(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
