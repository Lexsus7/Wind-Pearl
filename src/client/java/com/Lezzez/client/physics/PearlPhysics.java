package com.Lezzez.client.physics;
import net.minecraft.world.phys.Vec3;
public final class PearlPhysics {
    public static final double DRAG = 0.99, GRAVITY = 0.03;
    public static Vec3 predict(Vec3 p, Vec3 v, double ticks) {
        if (ticks <= 0) return p;
        double factor = DRAG * (-Math.expm1(ticks * Math.log(DRAG))) / (1 - DRAG);
        double gravity = DRAG * GRAVITY * (factor - ticks) / (1 - DRAG);
        return p.add(v.scale(factor)).add(0, gravity, 0);
    }
}
