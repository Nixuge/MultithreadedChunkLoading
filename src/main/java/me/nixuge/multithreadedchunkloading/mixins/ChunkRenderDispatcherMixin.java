package me.nixuge.multithreadedchunkloading.mixins;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListenableFutureTask;
import net.minecraft.client.renderer.RegionRenderCacheBuilder;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.client.renderer.chunk.RenderChunk;
import org.spongepowered.asm.mixin.*;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

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
    @Shadow
    private BlockingQueue<RegionRenderCacheBuilder> queueFreeRenderBuilders;
    @Shadow
    @Final
    private Queue<ListenableFutureTask<?>> queueChunkUploads = Queues.newArrayDeque();

    private final static Logger logger = Logger.getLogger("MultithreadedChunkLoading");

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(CallbackInfo ci) {
        // Note: overriding a final shadow.
        queueFreeRenderBuilders = Queues.newArrayBlockingQueue(100);
        for (int j = 0; j < 100; ++j) {
            queueFreeRenderBuilders.add(new RegionRenderCacheBuilder());
        }
        logger.info("Started up with " + queueFreeRenderBuilders.size() + " render builders.");

        for (int i = 0; i < 200; ++i) { // 200 is enough lmao
            ChunkRenderWorker chunkrenderworker = new ChunkRenderWorker((ChunkRenderDispatcher) (Object) this);
            Thread thread = threadFactory.newThread(chunkrenderworker);
            thread.start();
            this.listThreadedWorkers.add(chunkrenderworker);
        }
        logger.info("Started up with " + listThreadedWorkers.size() + " chunk render workers.");
    }

    /**
     * @author a
     * @reason b
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
     * @reason paddings
     */
    @Overwrite
    public String getDebugInfo() {
        return String.format("pC: %03d, pU: %03d, aB: %03d",
                this.queueChunkUpdates.size(),
                this.queueChunkUploads.size(),
                this.queueFreeRenderBuilders.size());
    }

    /**
     * 
     * /**
     * 
     * @author Nixuge
     * @reason queueChunkUploads full
     */
    @Overwrite
    public boolean runChunkUploads(long timeout) {
        boolean flag = false;

        while (true) {
            boolean flag1 = false;

            synchronized (this.queueChunkUploads) {
                if (!this.queueChunkUploads.isEmpty()) {
                    (this.queueChunkUploads.poll()).run();
                    flag1 = true;
                    flag = true;
                }
            }

            if (timeout == 0L || !flag1) {
                break;
            }
        }

        return flag;
    }
}
