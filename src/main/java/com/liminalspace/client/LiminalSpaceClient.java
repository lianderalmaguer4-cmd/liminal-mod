package com.liminalspace.client;

import com.liminalspace.LiminalSpaceMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class LiminalSpaceClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(LiminalSpaceMod.WHISPER, WhisperEntityRenderer::new);
    }
}
