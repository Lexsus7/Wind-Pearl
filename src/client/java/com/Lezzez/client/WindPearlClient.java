
package com.Lezzez.client;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import java.util.List;
public class WindPearlClient implements ClientModInitializer {

    private static KeyMapping activateKey;
    private static ThrownEnderpearl trackedPearl;
    private static boolean waitingForPearl;
    private static boolean autoShotPending;
    private static int searchTicks;
    private static int trackingTicks;
    private static int shotCooldownTicks;
    private static int autoWindSlot = -1;
    private static final double SINGLEPLAYER_DELAY = 3.0;
    private static final double MULTIPLAYER_DELAY = 5.0;

    private static final KeyMapping.Category WIND_PEARL_CATEGORY =
            KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath("windpearl", "controls")
            );
    @Override
    public void onInitializeClient() {
        activateKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.windpearl.activate",
                        InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, WIND_PEARL_CATEGORY)
        );
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (shotCooldownTicks > 0) shotCooldownTicks--;
            while (activateKey.consumeClick()) {checkInventory(client);}
            trackPearl(client);
        });
    }
    private void checkInventory(Minecraft client) {
        if (client.player == null || client.level == null) return;
        if (autoShotPending) return;

        int pearlSlot = -1;
        int windSlot = -1;

        var inventory = client.player.getInventory();

        for (int slot = 0; slot < 9; slot++) {
            var stack = inventory.getItem(slot);
            if (stack.is(Items.ENDER_PEARL)) pearlSlot = slot;
            if (stack.is(Items.WIND_CHARGE)) windSlot = slot;
        }
        if (pearlSlot == -1 || windSlot == -1) return;
        autoWindSlot = windSlot;
        autoShotPending = true;
        shotCooldownTicks = 0;
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
                    pearl -> pearl.getOwner() == client.player
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

        if (trackingTicks < 2 || trackingTicks > 20) return;
        if (!autoShotPending || shotCooldownTicks != 0) return;

        Vec3 windStart = client.player.getEyePosition();
        Vec3 position = trackedPearl.position();
        Vec3 velocity = trackedPearl.getDeltaMovement();

        double delay = client.hasSingleplayerServer()
                ? SINGLEPLAYER_DELAY
                : MULTIPLAYER_DELAY;

        TrajectoryCalculator.InterceptResult result =
                TrajectoryCalculator.calculateIntercept(
                        windStart, position, velocity, 1, 30, delay
                );
        if (result != null) {
            fireWindCharge(client, result);
        }
    }
    private void fireWindCharge(
            Minecraft client,
            TrajectoryCalculator.InterceptResult result
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
            client.player.setYRot(result.yaw());
            client.player.setXRot(result.pitch());

            inventory.setSelectedSlot(autoWindSlot);
            client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        } finally {
            inventory.setSelectedSlot(beforeSlot);
            client.player.setYRot(beforeYaw);
            client.player.setXRot(beforePitch);

            autoShotPending = false;
            shotCooldownTicks = 1;
        }
    }
}
