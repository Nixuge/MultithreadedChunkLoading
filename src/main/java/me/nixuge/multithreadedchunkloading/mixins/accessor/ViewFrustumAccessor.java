package me.nixuge.multithreadedchunkloading.mixins.accessor;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewFrustum.class)
public interface ViewFrustumAccessor {
    @Invoker("getRenderChunk")
    RenderChunk invokeGetRenderChunk(BlockPos pos);
}
