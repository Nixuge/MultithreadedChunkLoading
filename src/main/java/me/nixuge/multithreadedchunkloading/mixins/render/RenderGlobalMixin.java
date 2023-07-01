package me.nixuge.multithreadedchunkloading.mixins.render;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

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
     * @reason Why fucking not lol
     */
    @Overwrite
    public void updateChunks(long finishTimeNano) {
        this.displayListEntitiesDirty |= this.renderDispatcher.runChunkUploads(finishTimeNano);
//        if (finishTimeNano - System.nanoTime() < 0L) {
//            // This means the function is late already for proper render
//            return;
//        }

        if (!this.chunksToUpdate.isEmpty()) {
            Iterator<RenderChunk> iterator = this.chunksToUpdate.iterator();
            System.out.println(this.chunksToUpdate.size());
            int loopRan = 0;
            while (iterator.hasNext()) {
                RenderChunk renderchunk = iterator.next();

                loopRan++;
                if (!this.renderDispatcher.updateChunkLater(renderchunk)) {
//                    System.out.println("Broke due to full queue.");
                    break;
                }
//                this.renderDispatcher.updateChunkLater(renderchunk);

                renderchunk.setNeedsUpdate(false);
                iterator.remove();
//                long i = finishTimeNano - System.nanoTime();

                // original "late" system lmao
//                if (i < 0L) {
//                    System.out.println(finishTimeNano - System.nanoTime());
//                    break;
//                }
            }
//            System.out.println("loop ran: " + loopRan);
        }
    }
}
