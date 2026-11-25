package com.Lezzez.client.tracking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
public final class PlayerMotion {
    private static Object player;
    private static boolean previousGround, landing;
    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            player = null;
            landing = false;
            return;
        }
        boolean ground = client.player.onGround();
        landing = player == client.player && !previousGround && ground;
        player = client.player;
        previousGround = ground;
    }
     public static Vec3 inherited(Minecraft client, Vec3 pearlVelocity) {
        Vec3 v = client.player.getKnownMovement();
        boolean ground = client.player.onGround();
        double horizontalScale = ground ? (landing ? 1.10 : 1.85) : 1.0;
        double speed = Math.hypot(v.x, v.z);
        double pearlSpeed = Math.hypot(pearlVelocity.x, pearlVelocity.z);
        if (!ground && speed > 0.02 && pearlSpeed > 0.02) {
            double alignment = (v.x * pearlVelocity.x + v.z * pearlVelocity.z) / (speed * pearlSpeed);
            if (alignment < -0.8) horizontalScale = 1.10;
        }
        double verticalVelocity = ground ? 0.0 : v.y / 0.98 + 0.074;
        return new Vec3(v.x * horizontalScale, verticalVelocity, v.z * horizontalScale);
    }
}
