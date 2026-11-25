
package com.Lezzez.client;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;
import com.Lezzez.client.config.WindPearlConfig;
import com.Lezzez.client.physics.InterceptSolver;
import com.Lezzez.client.tracking.PlayerMotion;
public class WindPearlClient implements ClientModInitializer {

    private com.Lezzez.client.compat.AnalysisIntegration analysis;
    private boolean keyHeld, chargeHeld, manualShot;
    private Object lastWorld, lastPlayer;
    private final HashSet<UUID> fired = new HashSet<>();
    private final HashSet<UUID> active = new HashSet<>();
    private static ThrownEnderpearl trackedPearl;
    private static boolean waitingForPearl;
    private static boolean autoShotPending;
    private static int searchTicks;
    private static int trackingTicks;
    private static final int OBSERVATION_TICKS = 6;
    private Vec3 previousPearlPosition;
    private boolean verticalShot;
    private int motionSamples;
    private static int shotCooldownTicks;
    private static int autoWindSlot = -1;
    private static final double BASE_DELAY = 1.0;
    private final HashSet<UUID> oldPearls = new HashSet<>();

    @Override
    public void onInitializeClient() {
        WindPearlConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (analysis == null) analysis = new com.Lezzez.client.compat.AnalysisIntegration();
            PlayerMotion.tick(client);
            if (lastWorld != client.level || lastPlayer != client.player) {
                lastWorld = client.level; lastPlayer = client.player;
                fired.clear(); trackedPearl = null; waitingForPearl = false; autoShotPending = false;
                keyHeld = chargeHeld = false; shotCooldownTicks = 0;
            }
            if (client.player == null || client.level == null || client.gameMode == null) return;
            boolean gameplay = client.isWindowActive() && client.mouseHandler.isMouseGrabbed();
            int keyCode = WindPearlConfig.keyCode(WindPearlConfig.INSTANCE.key);
            boolean down = keyCode != -1 && InputConstants.isKeyDown(keyCode);
            boolean selected = client.player.getMainHandItem().is(Items.WIND_CHARGE);
            boolean press = down && !keyHeld;
            boolean select = selected && !chargeHeld;
            keyHeld = down; chargeHeld = selected;
            if (!gameplay) { autoShotPending = false; waitingForPearl = false; trackedPearl = null; return; }
            if (shotCooldownTicks > 0) shotCooldownTicks--;
            active.clear();
            for (var entity : client.level.entitiesForRendering()) if (entity instanceof ThrownEnderpearl) active.add(entity.getUUID());
            fired.retainAll(active);
            if (press) checkInventory(client);
            if (select && WindPearlConfig.INSTANCE.manualAutoThrow && !autoShotPending && !waitingForPearl && shotCooldownTicks == 0) beginManual(client);
            if (manualShot && autoShotPending && (!selected || !WindPearlConfig.INSTANCE.manualAutoThrow)) {
                trackedPearl = null; autoShotPending = false;
            }
            trackPearl(client);
        });
    }
    private void beginManual(Minecraft client) {
        ThrownEnderpearl newest = null;
        for (var entity : client.level.entitiesForRendering()) {
            if (entity instanceof ThrownEnderpearl pearl && pearl.getOwner() == client.player
                && !pearl.isRemoved() && !fired.contains(pearl.getUUID()) && pearl.tickCount <= 100
                && (newest == null || pearl.tickCount < newest.tickCount)) newest = pearl;
        }
        if (newest == null) return;
        trackedPearl = newest; waitingForPearl = false; autoShotPending = true; manualShot = true;
        autoWindSlot = client.player.getInventory().getSelectedSlot();
        trackingTicks = 0; motionSamples = 0; previousPearlPosition = null;
        Vec3 v = newest.getDeltaMovement();
        verticalShot = Math.hypot(v.x, v.z) < 0.03 && Math.hypot(client.player.getKnownMovement().x, client.player.getKnownMovement().z) <= 0.001;
    }
    private void checkInventory(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (autoShotPending || waitingForPearl || shotCooldownTicks > 0 || client.gameMode == null) return;

        int pearlSlot = -1;
        int windSlot = -1;

        var inventory = client.player.getInventory();

        for (int slot = 0; slot < 9; slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(Items.ENDER_PEARL)) pearlSlot = slot;
            if (stack.is(Items.WIND_CHARGE)) windSlot = slot;
        }
        if (pearlSlot == -1 || windSlot == -1) return;
        manualShot = false;
        autoWindSlot = windSlot;
        autoShotPending = true;
        shotCooldownTicks = 0;
        oldPearls.clear();
        for (var e : client.level.entitiesForRendering()) if (e instanceof ThrownEnderpearl) oldPearls.add(e.getUUID());
        verticalShot = Math.abs(client.player.getXRot()) >= 89.999f
                && Math.hypot(client.player.getKnownMovement().x, client.player.getKnownMovement().z) <= 0.001;
        throwPearl(client, pearlSlot);
    }
    private void throwPearl(Minecraft client, int pearlSlot) {
        if (client.player == null || client.gameMode == null) {
            autoShotPending = false;
            return;
        }
        var inventory = client.player.getInventory();
        int originalSlot = inventory.getSelectedSlot();

        try {
            inventory.setSelectedSlot(pearlSlot);
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        } finally {
            inventory.setSelectedSlot(originalSlot);
        }

        trackedPearl = null;
        waitingForPearl = true;
        searchTicks = 0;
        trackingTicks = 0;
        previousPearlPosition = null;
        motionSamples = 0;
    }
    private void trackPearl(Minecraft client) {
        if (client.player == null || client.level == null) {
            trackedPearl = null;
            waitingForPearl = false;
            autoShotPending = false;
            return;
        }
        if (waitingForPearl && trackedPearl == null) {
            searchTicks++;
            AABB searchArea = client.player.getBoundingBox().inflate(12.0);
            List<ThrownEnderpearl> pearls = client.level.getEntitiesOfClass(
                    ThrownEnderpearl.class,
                    searchArea,
                    pearl -> pearl.getOwner() == client.player && !oldPearls.contains(pearl.getUUID())
            );
            if (!pearls.isEmpty()) {
                trackedPearl = pearls.getFirst();
                waitingForPearl = false;
                trackingTicks = 0;
            } else if (searchTicks > 20) {
                waitingForPearl = false;
                autoShotPending = false;
            }
        }
        if (trackedPearl == null) return;
        if (trackedPearl.isRemoved()) {
            trackedPearl = null;
            autoShotPending = false;
            return;
        }

        trackingTicks++;

        if (trackingTicks > 20) { trackedPearl = null; autoShotPending = false; return; }
        Vec3 current = trackedPearl.position();
        if (previousPearlPosition != null) {
            Vec3 step = current.subtract(previousPearlPosition);
            if (step.length() > 0.01 && step.length() < 5.0) {
                motionSamples++;
            } else {
                motionSamples = 0;
            }
        }
        previousPearlPosition = current;
        if (verticalShot && Math.hypot(client.player.getKnownMovement().x, client.player.getKnownMovement().z) > 0.001) verticalShot = false;
        if (!autoShotPending || shotCooldownTicks != 0) return;
        if (fired.contains(trackedPearl.getUUID())) { autoShotPending = false; trackedPearl = null; return; }
        if (verticalShot ? trackingTicks < 2 : (trackingTicks < OBSERVATION_TICKS || motionSamples < 3)) return;
        if (trackingTicks >= 2) {
            double[] angles = analysis.angles(client, trackedPearl.getUUID());
            if (angles != null) { fireWindCharge(client, (float) angles[0], (float) angles[1]); return; }
        }

        Vec3 windStart = new Vec3(client.player.getX(), client.player.getEyeY(), client.player.getZ());
        Vec3 position = trackedPearl.position();
        Vec3 velocity = trackedPearl.getDeltaMovement();

        Vec3 playerVelocity = PlayerMotion.inherited(client, velocity);

        double delay = BASE_DELAY;
        if (!client.hasSingleplayerServer()) {
            var connection = client.getConnection();
            if (connection != null) {
                var info = connection.getPlayerInfo(client.player.getUUID());
                if (info != null) delay += Math.max(0, info.getLatency()) / 50.0;
            }
        }

        InterceptSolver.Result result =
                InterceptSolver.solve(
                        windStart, position, velocity, playerVelocity, delay, verticalShot, client
                );
        if (result != null) {
            fireWindCharge(client, result.yaw(), result.pitch());
        }
    }
    private void fireWindCharge(
            Minecraft client,
            float yaw, float pitch
    ) {
        if (client.player == null || client.gameMode == null) {
            autoShotPending = false;
            return;
        }
        if (autoWindSlot < 0 || autoWindSlot > 8) {
            autoShotPending = false;
            return;
        }

        var inventory = client.player.getInventory();

        if (!inventory.getItem(autoWindSlot).is(Items.WIND_CHARGE)) {
            autoShotPending = false;
            return;
        }

        float beforeYaw = client.player.getYRot();
        float beforePitch = client.player.getXRot();
        int beforeSlot = inventory.getSelectedSlot();

        try {
            client.player.setYRot(yaw);
            client.player.setXRot(pitch);

            inventory.setSelectedSlot(autoWindSlot);
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
            if (trackedPearl != null) fired.add(trackedPearl.getUUID());
        } finally {
            inventory.setSelectedSlot(beforeSlot);
            client.player.setYRot(beforeYaw);
            client.player.setXRot(beforePitch);

            autoShotPending = false;
            shotCooldownTicks = 1;
            trackedPearl = null;
        }
    }
}
