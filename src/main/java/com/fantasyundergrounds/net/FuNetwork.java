package com.fantasyundergrounds.net;

import com.fantasyundergrounds.FantasyUndergrounds;
import com.fantasyundergrounds.world.Environment;
import com.fantasyundergrounds.world.FuWorldgen;
import com.fantasyundergrounds.world.ZoneField;
import com.fantasyundergrounds.world.placement.InZoneFilter;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Optional server -> client channel for environment fog (overhaul spec, section 1). Every half
 * second the server works out which environment a player is standing in and tells the client
 * when it changes. The channel is optional: clients without this mod connect normally and keep
 * vanilla fog; nothing is sent to them.
 */
public final class FuNetwork {
    private FuNetwork() {}

    /** The player's current environment ({@link Environment} ordinal; ORDINARY = none). */
    public record EnvironmentPayload(int environment) implements CustomPacketPayload {
        public static final Type<EnvironmentPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(FantasyUndergrounds.MODID, "environment"));
        public static final StreamCodec<ByteBuf, EnvironmentPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(EnvironmentPayload::new, EnvironmentPayload::environment);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Last environment received by this client (read by the client fog handler). */
    public static volatile int clientEnvironment = Environment.ORDINARY.ordinal();

    private static final ResourceKey<Structure> RUINS = ResourceKey.create(Registries.STRUCTURE,
        Identifier.fromNamespaceAndPath(FantasyUndergrounds.MODID, "ancient_ruins"));
    private static final Map<ServerPlayer, Integer> SENT = new WeakHashMap<>();

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(EnvironmentPayload.TYPE, EnvironmentPayload.STREAM_CODEC,
            (payload, context) -> clientEnvironment = payload.environment());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) {
            return;
        }
        if (!player.connection.hasChannel(EnvironmentPayload.TYPE)) {
            return;
        }
        int env = environmentAt(player.level(), BlockPos.containing(player.getEyePosition())).ordinal();
        Integer last = SENT.get(player);
        if (last == null || last != env) {
            SENT.put(player, env);
            PacketDistributor.sendToPlayer(player, new EnvironmentPayload(env));
        }
    }

    /** The environment whose fog applies at a position (ORDINARY outside this mod's spaces). */
    public static Environment environmentAt(ServerLevel level, BlockPos pos) {
        if (!FuWorldgen.appliesTo(level.dimension()) || level.canSeeSky(pos)) {
            return Environment.ORDINARY;
        }
        if (level.getBiome(pos).is(InZoneFilter.KEEPS_OWN_DECORATION)) {
            return Environment.ORDINARY;
        }
        long seed = level.getSeed();
        if (FuWorldgen.canyons(seed).inside(pos.getX(), pos.getY(), pos.getZ(), 6.0)) {
            return Environment.GRAND_CANYONS;
        }
        Structure ruins = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(RUINS);
        if (ruins != null && level.structureManager().getStructureWithPieceAt(pos, ruins).isValid()) {
            return Environment.ANCIENT_RUINS;
        }
        ZoneField.Slot slot = FuWorldgen.zones(seed).sample(pos.getX(), pos.getY(), pos.getZ()).slot();
        if (slot.isCavern() && pos.getY() > slot.floorY() - 8 && pos.getY() < slot.ceilY() + 8) {
            return slot.islands() > 0 ? Environment.FLOATING_ISLANDS : slot.env();
        }
        return Environment.ORDINARY;
    }
}
