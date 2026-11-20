package com.Lezzez.client;
import net.minecraft.world.phys.Vec3;

public class TrajectoryCalculator {

    private static final double PEARL_GRAVITY = 0.03;
    private static final double PEARL_DRAG = 0.99;
    private static final double WIND_SPEED = 1.7773877;
    private static final double MIN_DISTANCE = 20.0;
    private static final double MAX_DISTANCE = 40.0;
    private static final double MAX_ERROR = 0.5;
    private static final double STEP = 0.05;

    private static Vec3 predictFractional(Vec3 position, Vec3 velocity, double ticks) {
        int wholeTicks = (int) Math.floor(ticks);
        double fraction = ticks - wholeTicks;

        for (int i = 0; i < wholeTicks; i++) {
            position = position.add(velocity);
            velocity = velocity.scale(PEARL_DRAG).add(0, -PEARL_GRAVITY, 0);
        }
        return position.add(velocity.scale(fraction));
    }
    private static float[] calculateAngles(Vec3 from, Vec3 target) {
        Vec3 difference = target.subtract(from);
        double horizontal = Math.sqrt(
                difference.x * difference.x + difference.z * difference.z
        );
        float yaw = (float) Math.toDegrees(Math.atan2(-difference.x, difference.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(difference.y, horizontal));
        return new float[]{yaw, pitch};
    }
    public record InterceptResult(
            float yaw, float pitch, int ticks, double distance, Vec3 target
    ) {}
    public static InterceptResult calculateIntercept(
            Vec3 windStart, Vec3 pearlPosition, Vec3 pearlVelocity,
            int minTicks,
            int maxTicks,
            double launchDelayTicks
    ) {
        if (launchDelayTicks < 0 || minTicks < 0 || maxTicks < minTicks) {
            throw new IllegalArgumentException("invalid tick range or delay");
        }

        InterceptResult best = null;
        double bestError = Double.MAX_VALUE;

        int steps = (int) Math.ceil((maxTicks - minTicks) / STEP);

        for (int i = 0; i <= steps; i++) {
            double flightTicks = Math.min(maxTicks, minTicks + i * STEP);
            if (flightTicks <= 0) continue;
            Vec3 target = predictFractional(
                    pearlPosition, pearlVelocity, launchDelayTicks + flightTicks
            );
            double distance = windStart.distanceTo(target);
            if (distance < MIN_DISTANCE || distance > MAX_DISTANCE) continue;

            double error = Math.abs(distance - WIND_SPEED * flightTicks);

            if (error < bestError) {
                float[] angles = calculateAngles(windStart, target);
                best = new InterceptResult(angles[0], angles[1],
                        (int) Math.round(flightTicks),
                        distance, target
                );

                bestError = error;
            }
        }
        return bestError <= MAX_ERROR ? best : null;
    }
}
