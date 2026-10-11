package com.liminalspace.client;

import com.liminalspace.LiminalSpaceMod;
import com.liminalspace.entity.WhisperEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Axis;
import org.joml.Matrix4f;

public class WhisperEntityRenderer extends EntityRenderer<WhisperEntity, WhisperEntityRenderer.WhisperRenderState> {
    private static final Identifier BODY = Identifier.of(LiminalSpaceMod.MOD_ID, "textures/entity/body.png");
    private static final Identifier HEAD = Identifier.of(LiminalSpaceMod.MOD_ID, "textures/entity/head.png");
    private static final Identifier JAW = Identifier.of(LiminalSpaceMod.MOD_ID, "textures/entity/jaw.png");
    private static final Identifier HAND_LEFT = Identifier.of(LiminalSpaceMod.MOD_ID, "textures/entity/hand_left.png");
    private static final Identifier HAND_RIGHT = Identifier.of(LiminalSpaceMod.MOD_ID, "textures/entity/hand_right.png");

    public WhisperEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
        this.shadowOpacity = 0.0f;
    }

    @Override
    public WhisperRenderState createRenderState() {
        return new WhisperRenderState();
    }

    @Override
    public void updateRenderState(WhisperEntity entity, WhisperRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        state.animationTime = entity.age + tickDelta;
        state.hostile = entity.isHostile();
    }

    @Override
    public void render(WhisperRenderState state, MatrixStack matrices, VertexConsumerProvider providers, int light) {
        float t = state.animationTime;
        float speed = state.hostile ? 1.9f : 1.0f;
        float tic = (float) Math.sin(t * 0.37f * speed) * (state.hostile ? 0.085f : 0.035f);
        float tic2 = (float) Math.sin(t * 0.91f * speed + 2.4f) * (state.hostile ? 0.12f : 0.055f);
        float jawTick = Math.max(0f, (float) Math.sin(t * 0.18f * speed)) * (state.hostile ? 0.28f : 0.08f);

        matrices.push();
        // Billboard: the creature is a flat 2D apparition that always faces the camera.
        matrices.multiply(net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera().getRotation());

        drawSprite(matrices, providers, BODY, 1.85f, 3.85f, 0.0f, 0.0f, 0.0f, light);

        matrices.push();
        matrices.translate(tic2, 3.95f, 0.0);
        matrices.multiply(Axis.Z_POSITIVE.rotation(tic));
        drawSprite(matrices, providers, HEAD, 1.55f, 1.95f, 0.0f, -0.05f, 0.0f, light);
        matrices.pop();

        matrices.push();
        matrices.translate(tic2 * 0.55f, 3.48f - jawTick, 0.0);
        matrices.multiply(Axis.Z_POSITIVE.rotation(-tic * 0.5f));
        drawSprite(matrices, providers, JAW, 1.18f, 0.68f, 0.0f, 0.0f, 0.0f, light);
        matrices.pop();

        matrices.push();
        matrices.translate(-0.88f + tic, 2.65f, 0.0);
        matrices.multiply(Axis.Z_POSITIVE.rotation(tic2));
        drawSprite(matrices, providers, HAND_LEFT, 0.82f, 1.25f, 0.0f, -0.62f, 0.0f, light);
        matrices.pop();

        matrices.push();
        matrices.translate(0.88f - tic2, 2.63f, 0.0);
        matrices.multiply(Axis.Z_POSITIVE.rotation(-tic));
        drawSprite(matrices, providers, HAND_RIGHT, 0.82f, 1.25f, 0.0f, -0.62f, 0.0f, light);
        matrices.pop();

        matrices.pop();
    }

    public static class WhisperRenderState extends EntityRenderState {
        public float animationTime;
        public boolean hostile;
    }

    private static void drawSprite(MatrixStack matrices, VertexConsumerProvider providers, Identifier texture,
                                    float width, float height, float x, float y, float z, int light) {
        VertexConsumer v = providers.getBuffer(RenderLayer.getEntityTranslucent(texture));
        Matrix4f m = matrices.peek().getPositionMatrix();
        float x0 = x - width / 2.0f;
        float x1 = x + width / 2.0f;
        float y0 = y;
        float y1 = y + height;

        v.vertex(m, x0, y0, z, 0xFFFFFFFF, 0, 1, OverlayTexture.DEFAULT_UV, light, 0, 0, 1);
        v.vertex(m, x1, y0, z, 0xFFFFFFFF, 1, 1, OverlayTexture.DEFAULT_UV, light, 0, 0, 1);
        v.vertex(m, x1, y1, z, 0xFFFFFFFF, 1, 0, OverlayTexture.DEFAULT_UV, light, 0, 0, 1);
        v.vertex(m, x0, y1, z, 0xFFFFFFFF, 0, 0, OverlayTexture.DEFAULT_UV, light, 0, 0, 1);
    }
}
