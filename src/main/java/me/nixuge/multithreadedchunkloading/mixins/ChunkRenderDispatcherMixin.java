package me.nixuge.multithreadedchunkloading.mixins;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.*;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.EnumWorldBlockLayer;
import org.spongepowered.asm.mixin.*;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

@Mixin(ChunkRenderDispatcher.class)
public class ChunkRenderDispatcherMixin {
    @Shadow
    @Final
    private static ThreadFactory threadFactory;

    @Shadow
    @Final
    private List<ChunkRenderWorker> listThreadedWorkers;

    @Shadow
    private BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates;

//    @Shadow
//    private BlockingQueue<RegionRenderCacheBuilder> queueFreeRenderBuilders;

    private static BlockingQueue<RegionRenderCacheBuilder> newQueueFreeRenderBuilders;

    @Shadow
    @Final
    private Queue<ListenableFutureTask<?>> queueChunkUploads = Queues.newArrayDeque();

    @Shadow
    public void clearChunkUpdates() {
    }

    @Shadow
    public boolean runChunkUploads(long p_178516_1_) {
        return false;
    }

    @Shadow
    private void uploadDisplayList(WorldRenderer p_178510_1_, int p_178510_2_, RenderChunk chunkRenderer) {
    }

    @Shadow
    private void uploadVertexBuffer(WorldRenderer p_178506_1_, VertexBuffer vertexBufferIn) {
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(CallbackInfo ci) {
        newQueueFreeRenderBuilders = Queues.newArrayBlockingQueue(50);

        for (int i = 0; i < 200; ++i) { // 200 is enough lmao
            ChunkRenderWorker chunkrenderworker = new ChunkRenderWorker((ChunkRenderDispatcher) (Object) this);
            Thread thread = threadFactory.newThread(chunkrenderworker);
            thread.start();
            this.listThreadedWorkers.add(chunkrenderworker);
        }
        System.out.println("Runningt: " + listThreadedWorkers.size());

        for (int j = 0; j < 50; ++j) {
            newQueueFreeRenderBuilders.add(new RegionRenderCacheBuilder());
        }
        System.out.println("RunningQ: " + newQueueFreeRenderBuilders.size());
    }

    /**
     * @author Nixuge
     * @reason Optimizes a bit to avoid calling unnecessary functions, instead just adding a check at the start
     */
    @Overwrite
    public boolean updateChunkLater(RenderChunk chunkRenderer) {
        if (queueChunkUpdates.remainingCapacity() == 0) {
            return false;
        }

        final ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskChunk();
        chunkcompiletaskgenerator.addFinishRunnable(new Runnable() {
            public void run() {
                queueChunkUpdates.remove(chunkcompiletaskgenerator);
            }
        });

        queueChunkUpdates.add(chunkcompiletaskgenerator);

        return true;
    }

    /**
     * @author Nixuge
     * @reason newQueueFreeRenderBuilders
     */
    @Overwrite
    public String getDebugInfo() {
        return String.format("pC: %03d, pU: %03d, aB: %02d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), newQueueFreeRenderBuilders.size());
    }

    /**
     * @author Nixuge
     * @reason newQueueFreeRenderBuilders
     */
    @Overwrite
    public void stopChunkUpdates() {
        this.clearChunkUpdates();

        while (this.runChunkUploads(0L)) {

        }

        List<RegionRenderCacheBuilder> list = Lists.newArrayList();

        while (list.size() != 5) {
            try {
                list.add(this.allocateRenderBuilder());
            } catch (InterruptedException ignored) {

            }
        }

        newQueueFreeRenderBuilders.addAll(list);
    }

    /**
     * @author Nixuge
     * @reason newQueueFreeRenderBuilders
     */
    @Overwrite
    public void freeRenderBuilder(RegionRenderCacheBuilder p_178512_1_) {
        newQueueFreeRenderBuilders.add(p_178512_1_);
    }

    /**
     * @author Nixuge
     * @reason newQueueFreeRenderBuilders
     */
    @Overwrite
    public RegionRenderCacheBuilder allocateRenderBuilder() throws InterruptedException {
        return newQueueFreeRenderBuilders.take();
    }

    // Technically runs an unneccessary "if", but oh well
    @Redirect(method = "runChunkUploads", at = @At(value = "INVOKE", target = "Ljava/lang/System;nanoTime()J"))
    public long runChunkUploadsBypassNanoTime() {
        return 0;
    }

    /**
     * @author Nixuge
     * @reason optifine
     */
    @Overwrite
    public ListenableFuture<Object> uploadChunk(final EnumWorldBlockLayer player, final WorldRenderer p_178503_2_, final RenderChunk chunkRenderer, final CompiledChunk compiledChunkIn) {
        if (Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            if (OpenGlHelper.useVbo()) {
                this.uploadVertexBuffer(p_178503_2_, chunkRenderer.getVertexBufferByLayer(player.ordinal()));
            } else {
                this.uploadDisplayList(p_178503_2_, ((ListedRenderChunk) chunkRenderer).getDisplayList(player, compiledChunkIn), chunkRenderer);
            }

            p_178503_2_.setTranslation(0.0D, 0.0D, 0.0D);
            return Futures.immediateFuture(null);
        } else {
            ListenableFutureTask<Object> listenablefuturetask = ListenableFutureTask.create(new Runnable() {
                public void run() {
                    uploadChunk(player, p_178503_2_, chunkRenderer, compiledChunkIn);
                }
            }, null);

            synchronized (this.queueChunkUploads) {
                this.queueChunkUploads.add(listenablefuturetask);
                return listenablefuturetask;
            }
        }
    }
}