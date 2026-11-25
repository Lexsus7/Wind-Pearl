package com.Lezzez.client.compat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.Collection;
import com.Lezzez.client.config.WindPearlConfig;

public final class AnalysisIntegration {
    private Object tracker;
    private Method tracks, owner, uuid, resolve;
    private boolean available;
    public AnalysisIntegration() {
        if (!FabricLoader.getInstance().isModLoaded("pearlwind_chargeanalysis")) return;
        try {
            String p = "com.example.pearlwindchargeanalysis.";
            tracker = Class.forName(p + "PearlWindChargeAnalysisClient").getField("TRACKER").get(null);
            tracks = tracker.getClass().getMethod("getFlyingPearls");
            Class<?> track = Class.forName(p + "PearlTrack");
            owner = track.getMethod("ownerUuid"); uuid = track.getMethod("uuid");
            resolve = Class.forName(p + "AimTargetProvider").getMethod("resolve", int.class);
            available = true;
        } catch (ReflectiveOperationException | LinkageError ignored) { }
    }
    public double[] angles(Minecraft client, UUID target) {
        if (!available || !WindPearlConfig.INSTANCE.useAnalysis || client.player == null) return null;
        try {
            int index = 0;
            for (Object track : (Collection<?>) tracks.invoke(tracker)) {
                index++;
                if (!target.equals(uuid.invoke(track)) || !client.player.getUUID().equals(owner.invoke(track))) continue;
                double[] a = (double[]) resolve.invoke(null, index);
                return a != null && a.length >= 2 && Double.isFinite(a[0]) && Double.isFinite(a[1]) && Math.abs(a[1]) <= 90 ? a : null;
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) { available = false; }
        return null;
    }
}
