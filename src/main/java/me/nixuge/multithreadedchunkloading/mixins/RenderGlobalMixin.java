package me.nixuge.multithreadedchunkloading.mixins;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.Set;

@Mixin(RenderGlobal.class)
public class RenderGlobalMixin {
    @Shadow
    private boolean displayListEntitiesDirty;
    @Final
    @Shadow
    private ChunkRenderDispatcher renderDispatcher;
    @Shadow
    private Set<RenderChunk> chunksToUpdate;

    /**
     * @author Nixuge
     * @reason Refactored from decompile & removed the "late" check
     */
    @Overwrite
    public void updateChunks(long finishTimeNano) {
        this.displayListEntitiesDirty |= this.renderDispatcher.runChunkUploads(finishTimeNano);

        if (!this.chunksToUpdate.isEmpty()) {
            Iterator<RenderChunk> iterator = this.chunksToUpdate.iterator();

            while (iterator.hasNext()) {
                RenderChunk renderchunk = iterator.next();

                if (!this.renderDispatcher.updateChunkLater(renderchunk)) {
                    break;
                }

                renderchunk.setNeedsUpdate(false);
                iterator.remove();
            }
        }
    }
}
