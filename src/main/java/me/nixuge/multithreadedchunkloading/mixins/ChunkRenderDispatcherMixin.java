package me.nixuge.multithreadedchunkloading.mixins;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFutureTask;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.chunk.*;
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

//    @Shadow
//    private BlockingQueue<ChunkCompileTaskGenerator> queueChunkUpdates;

//    @Shadow
//    private BlockingQueue<RegionRenderCacheBuilder> queueFreeRenderBuilders;

    // Not sure if this helps a lot, but the mod is already ram hungry
    // Might as well just in case
    private static final BlockingQueue<ChunkCompileTaskGenerator> newQueueChunkUpdates = Queues.newArrayBlockingQueue(1000);

    private static BlockingQueue<RegionRenderCacheBuilder> newQueueFreeRenderBuilders;

    @Shadow
    @Final
    private Queue<ListenableFutureTask<?>> queueChunkUploads = Queues.newArrayDeque();


    @Shadow
    public boolean runChunkUploads(long p_178516_1_) {
        return false;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(CallbackInfo ci) {
        // queueChunkUpdates
//        newQueueChunkUpdates =

        // listThreadedWorkers
        for (int i = 0; i < 200; ++i) { // 200 is enough lmao
            ChunkRenderWorker chunkrenderworker = new ChunkRenderWorker((ChunkRenderDispatcher) (Object) this);
            Thread thread = threadFactory.newThread(chunkrenderworker);
            thread.start();
            this.listThreadedWorkers.add(chunkrenderworker);
        }
        System.out.println("Runningt: " + listThreadedWorkers.size());

        // queueFreeRenderBuilders
        newQueueFreeRenderBuilders = Queues.newArrayBlockingQueue(50);
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
        if (newQueueChunkUpdates.remainingCapacity() == 0) {
//            System.out.println("returning because full!");
            return false;
        }

        final ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskChunk();
        chunkcompiletaskgenerator.addFinishRunnable(() -> newQueueChunkUpdates.remove(chunkcompiletaskgenerator));

        newQueueChunkUpdates.add(chunkcompiletaskgenerator);

        return true;
    }

//    DOESN'T WORK: USELESS.
    /*
     * @author
     * @reason
     */
//    @Overwrite
//    public boolean updateChunkNow(RenderChunk chunkRenderer)
//    {
//        System.out.println("debug?");
//        return updateChunkLater(chunkRenderer);
//    }

    /**
     * @author Nixuge
     * @reason newQueueChunkUpdates
     */
    @Overwrite
    public ChunkCompileTaskGenerator getNextChunkUpdate() throws InterruptedException {
        return newQueueChunkUpdates.take();
    }

    /**
     * @author Nixuge
     * @reason newQueueChunkUpdates
     */
    @Overwrite
    public boolean updateTransparencyLater(RenderChunk chunkRenderer) {
        chunkRenderer.getLockCompileTask().lock();

        try
        {
            final ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskTransparency();

            if (chunkcompiletaskgenerator != null)
            {
                chunkcompiletaskgenerator.addFinishRunnable(() -> newQueueChunkUpdates.remove(chunkcompiletaskgenerator));
                return newQueueChunkUpdates.offer(chunkcompiletaskgenerator);
            }
        }
        finally
        {
            chunkRenderer.getLockCompileTask().unlock();
        }

        return true;
    }

    /**
     * @author Nixuge
     * @reason newQueueChunkUpdates
     */
    @Overwrite
    public void clearChunkUpdates() {
        while (!newQueueChunkUpdates.isEmpty())
        {
            ChunkCompileTaskGenerator chunkcompiletaskgenerator = newQueueChunkUpdates.poll();

            if (chunkcompiletaskgenerator != null)
            {
                chunkcompiletaskgenerator.finish();
            }
        }
    }

    /*
     * OPTIFINE FUNCTION,
     * CAN'T BE MIXINED INTO HERE!
     * @author Nixuge
     * @reason newQueueChunkUpdates
     */
//    @Overwrite
//    public boolean hasChunkUpdates() {
//        return newQueueChunkUpdates.isEmpty() && this.queueChunkUploads.isEmpty();
//    }









    /**
     * @author Nixuge
     * @reason newQueueFreeRenderBuilders
     */
    @Overwrite
    public String getDebugInfo() {
        return String.format("pC: %04d/%04d, pU: %03d, aB: %02d", newQueueChunkUpdates.remainingCapacity(), newQueueChunkUpdates.size(), this.queueChunkUploads.size(), newQueueFreeRenderBuilders.size());
    }

    /**
     * Note:
     * Due to optifine, I have to for some reason
     * unfortunately overwrite the whole thing.
     * I can't just redirect the list call :/
     * @author Nixuge
     * @reason newQueueFreeRenderBuilders
     */
//    @Redirect(method = "stopChunkUpdates", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/BlockingQueue;addAll(Ljava/util/Collection;)Z"))
//    public boolean stopChunkUpdates(BlockingQueue<RegionRenderCacheBuilder> instance, Collection<RegionRenderCacheBuilder> list) {
//        return newQueueFreeRenderBuilders.addAll(list);
//    }
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
}