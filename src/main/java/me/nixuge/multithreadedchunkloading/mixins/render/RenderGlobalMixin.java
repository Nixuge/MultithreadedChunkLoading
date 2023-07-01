package me.nixuge.multithreadedchunkloading.mixins.render;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(RenderGlobal.class)
public class RenderGlobalMixin {
    @Final
    @Shadow
    private ChunkRenderDispatcher renderDispatcher;
    @Shadow
    private Set<RenderChunk> chunksToUpdate;

    /**
     * Why is this injected here?
     * At first glance it indeed seems weird, it looks like I'm replacing the entire function anyways,
     * so why not inject at head or just overwrite?
     * The answer to that: Optifine.
     * When optifine is injected this function runs 3 loops (unlike 1 in vanilla):
     * - if (this.chunksToUpdateForced.size() > 0)
     * - if (this.chunksToResortTransparency.size() > 0)
     * - if (!this.chunksToUpdate.isEmpty()) (the vanilla one we want to inject into)
     * Thankfully no need to keep a counter of the size() calls or smth, since the vanilla function,
     * unlike the OF ones, uses isEmpty()
     * So we just let OF run the first 2 loops (which don't return anyways), then just
     * "overwrite" the last one by injecting at its start & cancelling right after
     * so the real one doesn't run
     */
    @Inject(method = "updateChunks", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z"), cancellable = true)
    public void updateChunksVanillaOptifine(long finishTimeNano, CallbackInfo ci) {
        if (!this.chunksToUpdate.isEmpty()) {
            Iterator<RenderChunk> iterator = this.chunksToUpdate.iterator();
//            int i = 0;
//            int origSize = this.chunksToUpdate.size();
            while (iterator.hasNext()) {
//                i++;
                RenderChunk renderchunk = iterator.next();

                if (!this.renderDispatcher.updateChunkLater(renderchunk)) {
                    break;
                }

                renderchunk.setNeedsUpdate(false);
                iterator.remove();
            }
//            System.out.println("Ran: " + i + "x, size: " + this.chunksToUpdate.size() + "/" + origSize);
//            System.out.println("Size left: " + this.chunksToUpdate.size());
        }
        ci.cancel();
    }
}
