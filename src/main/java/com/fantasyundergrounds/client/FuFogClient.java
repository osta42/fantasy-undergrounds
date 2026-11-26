package com.fantasyundergrounds.client;

import com.fantasyundergrounds.FantasyUndergrounds;
import com.fantasyundergrounds.config.FuClientConfig;
import com.fantasyundergrounds.net.FuNetwork;
import com.fantasyundergrounds.world.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Environment fog (client only, optional). Each environment gets its own fog colour and density;
 * the change fades in and out over about two seconds. Fog only gets denser, never thinner, than
 * what the game already uses, and only in open air (water, lava and powder snow keep vanilla
 * fog; Drowned Abysses only tint the water fog). Disabled by {@code environmentFog = false} in
 * the client config, and inactive whenever the server does not have this mod.
 */
@EventBusSubscriber(modid = FantasyUndergrounds.MODID, value = Dist.CLIENT)
public final class FuFogClient {
    /** Fog end distance in blocks and colour, per environment ordinal. */
    private static final float[][] FOG = new float[Environment.values().length][];

    static {
        set(Environment.GRAND_CAVERNS, 230, 0.09F, 0.10F, 0.13F);
        set(Environment.FLOATING_ISLANDS, 250, 0.10F, 0.12F, 0.15F);
        set(Environment.UNDERGROUND_FORESTS, 150, 0.07F, 0.13F, 0.08F);
        set(Environment.CRYSTAL_HOLLOWS, 130, 0.15F, 0.08F, 0.21F);
        set(Environment.INFERNAL_CHASMS, 120, 0.32F, 0.10F, 0.03F);
        set(Environment.DROWNED_ABYSSES, 130, 0.03F, 0.12F, 0.15F);
        set(Environment.ROOT_SYSTEMS, 130, 0.12F, 0.09F, 0.05F);
        set(Environment.GRAND_CANYONS, 260, 0.11F, 0.11F, 0.12F);
        set(Environment.ANCIENT_RUINS, 140, 0.04F, 0.11F, 0.13F);
    }

    private static void set(Environment env, float end, float r, float g, float b) {
        FOG[env.ordinal()] = new float[] {end, r, g, b};
    }

    private static float amount;                  // 0 = vanilla, 1 = full environment fog
    private static final float[] current = {230, 0.09F, 0.10F, 0.13F};

    private FuFogClient() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        float[] target = mc.level == null || !FuClientConfig.ENVIRONMENT_FOG.get() ? null : FOG[FuNetwork.clientEnvironment];
        if (target == null) {
            amount = Math.max(0F, amount - 0.025F);
            if (mc.level == null) {
                FuNetwork.clientEnvironment = Environment.ORDINARY.ordinal();
            }
            return;
        }
        amount = Math.min(1F, amount + 0.025F);
        for (int i = 0; i < 4; i++) {
            current[i] += (target[i] - current[i]) * 0.05F;
        }
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (amount <= 0F || event.getType() != FogType.ATMOSPHERIC) {
            return;
        }
        float strength = FuClientConfig.FOG_STRENGTH.get().floatValue();
        if (strength <= 0F) {
            return;
        }
        float end = current[0] / strength;
        float far = event.getFarPlaneDistance();
        if (end < far) {
            event.setFarPlaneDistance(far + (end - far) * amount);
            // Fog starts a third of the way out: near things stay crisp, distance fades into haze.
            event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), event.getFarPlaneDistance() * 0.33F));
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (amount <= 0F) {
            return;
        }
        float a = Math.min(1F, amount * FuClientConfig.FOG_STRENGTH.get().floatValue());
        if (event.getCamera().getFluidInCamera() == FogType.WATER) {
            if (FuNetwork.clientEnvironment != Environment.DROWNED_ABYSSES.ordinal()) {
                return;
            }
            a *= 0.6F;
        }
        event.setRed(event.getRed() + (current[1] - event.getRed()) * a);
        event.setGreen(event.getGreen() + (current[2] - event.getGreen()) * a);
        event.setBlue(event.getBlue() + (current[3] - event.getBlue()) * a);
    }
}
