package com.example.invisiblehitboxes;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/**
 * Client-side, purely visual. Outlines the hitbox of every INVISIBLE entity near you (players with the
 * Invisibility effect, invisible armor stands, invisible mobs). Nothing is sent to the server, and nothing
 * about gameplay changes. The server only sends entities that are inside your normal render/tracking
 * distance, so that is the limit of what can be shown.
 *
 * Press H (rebindable) to toggle. It starts OFF every launch.
 */
public class InvisibleHitboxesClient implements ClientModInitializer {
    public static final String MOD_ID = "invisiblehitboxes";

    private static final double RANGE = 64.0;          // blocks
    private static final int COLOR = 0xFFFF3030;       // ARGB, red
    private static final int COLOR_OFFSET = 0xFF30FF30; // green: drawn with the camera offset applied
    private static final float LINE_WIDTH = 3.0f;
    private static int foundCount = 0;
    private static int tickCounter = 0;

    private static boolean enabled = false;
    private static KeyMapping toggleKey;
    private static RenderType linesType;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.invisiblehitboxes.toggle", InputConstants.KEY_H, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (toggleKey.consumeClick()) {
                enabled = !enabled;
                if (mc.player != null) {
                    mc.player.sendSystemMessage(Component.literal(
                            "[Invisible Hitboxes] " + (enabled ? "ON" : "OFF")));
                }
            }
            // Diagnostic: every 5 seconds show how many invisible entities the game currently sees.
            if (enabled && mc.player != null && ++tickCounter % 100 == 0) {
                mc.player.sendSystemMessage(Component.literal(
                        "[Invisible Hitboxes] invisible entities found: " + foundCount));
            }
        });

        LevelRenderEvents.AFTER_SOLID_FEATURES.register(ctx -> {
            Minecraft mc = Minecraft.getInstance();
            float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
            render(ctx.poseStack(), ctx.submitNodeCollector(), partial);
        });
    }

    private static void render(PoseStack pose, SubmitNodeCollector collector, float partial) {
        if (!enabled || pose == null || collector == null) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer self = mc.player;
        ClientLevel level = mc.level;
        if (self == null || level == null) return;

        if (linesType == null) {
            linesType = RenderType.create("invisiblehitboxes_lines",
                    RenderSetup.builder(RenderPipelines.LINES).createRenderSetup());
        }

        List<Entity> nearby = level.getEntities(self, self.getBoundingBox().inflate(RANGE),
                e -> e.isInvisible() && e.isAlive());

        foundCount = nearby.size();
        Vec3 cam = mc.gameRenderer.mainCamera().position();

        for (Entity e : nearby) {
            // Interpolate so the box moves smoothly with the entity between ticks.
            Vec3 delta = e.getPosition(partial).subtract(e.position());
            AABB box = e.getBoundingBox().move(delta);

            // Red: drawn with the pose exactly as given.
            collector.submitShapeOutline(pose, Shapes.create(box), linesType, COLOR, LINE_WIDTH, false);

            // Green: same box with the camera position subtracted (in case the pose is not camera-relative).
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            collector.submitShapeOutline(pose, Shapes.create(box), linesType, COLOR_OFFSET, LINE_WIDTH, false);
            pose.popPose();
        }
    }
}
