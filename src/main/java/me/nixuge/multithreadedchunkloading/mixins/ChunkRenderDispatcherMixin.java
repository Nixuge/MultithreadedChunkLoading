package me.nixuge.multithreadedchunkloading.mixins;

import com.google.common.collect.Lists;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;

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

    private static final BlockingQueue<RegionRenderCacheBuilder> newQueueFreeRenderBuilders = Queues.newArrayBlockingQueue(100);

    @Shadow
    @Final
    private Queue<ListenableFutureTask<?>> queueChunkUploads = Queues.newArrayDeque();

    @Shadow
    public void clearChunkUpdates() {
    }

    ;


    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(CallbackInfo ci) {
//        newQueueFreeRenderBuilders = Queues.newArrayBlockingQueue(50);
        System.out.println(newQueueFreeRenderBuilders);
//        queueFreeRenderBuilders = Queues.newArrayBlockingQueue(5);
//        try {
            System.out.println("==========");
//            Field f = ReflectionUtils.findField(queueChunkUpdates.getClass(), int.class);
//            Field[] fields = queueChunkUpdates.getClass().getDeclaredFields();
//            System.out.println(queueChunkUpdates);
//            for(Field f : fields) {
//                f.setAccessible(true);
//                System.out.println(f.getName() + ": " + f.getType().getName() + "(" + f.get(queueChunkUpdates) + ")");
//            }
//            System.out.println("done");
//            f.set(this, Queues.<ChunkCompileTaskGenerator>newArrayBlockingQueue(100));
//            System.out.println(f.getName());
//        } catch (Exception e) {
//            System.out.println("owo " + e);
//        }
        for (int i = 0; i < 200; ++i) { // 200 is enough lmao
            ChunkRenderWorker chunkrenderworker = new ChunkRenderWorker((ChunkRenderDispatcher) (Object) this);
            Thread thread = threadFactory.newThread(chunkrenderworker);
            thread.start();
            this.listThreadedWorkers.add(chunkrenderworker);
        }
        System.out.println("Runningt: " + listThreadedWorkers.size());

        for (int j = 0; j < 100; ++j)
        {
            newQueueFreeRenderBuilders.add(new RegionRenderCacheBuilder());
        }
        System.out.println("RunningQ: " + newQueueFreeRenderBuilders.size());
    }

    /**
     * @author a
     * @reason b
     */
    @Overwrite
    public boolean updateChunkLater(RenderChunk chunkRenderer) {
        if (queueChunkUpdates.remainingCapacity() == 0) {
//            System.out.println("queueChunkUpdates full, returning.");
            return false;
        }

        final ChunkCompileTaskGenerator chunkcompiletaskgenerator = chunkRenderer.makeCompileTaskChunk();
        chunkcompiletaskgenerator.addFinishRunnable(new Runnable() {
            public void run() {
                queueChunkUpdates.remove(chunkcompiletaskgenerator);
            }
        });

        queueChunkUpdates.add(chunkcompiletaskgenerator);

//        if (!flag) {
//            chunkcompiletaskgenerator.finish();
//        }

//        flag1 = flag;


//        return flag1;
        return true;
    }

    /**
     * @author Nixuge
     * @reason newQueueFreeRenderBuilders
     */
    @Overwrite
    public String getDebugInfo() {
        return String.format("pC: %03d, pU: %1d, aB: %3d", this.queueChunkUpdates.size(), this.queueChunkUploads.size(), newQueueFreeRenderBuilders.size());
    }

    /**
     * @author Nixuge
     * @reason newQueueFreeRenderBuilders
     */
    @Overwrite
    public void stopChunkUpdates() {
        this.clearChunkUpdates();

        while (this.runChunkUploads(0L)) {
            ;
        }

        List<RegionRenderCacheBuilder> list = Lists.<RegionRenderCacheBuilder>newArrayList();

        while (list.size() != 5) {
            try {
                list.add(this.allocateRenderBuilder());
            } catch (InterruptedException ignored) {
                ;
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

    /**
     * @author Nixuge
     * @reason queueChunkUploads full
     */
    @Overwrite
    public boolean runChunkUploads(long p_178516_1_)
    {
        boolean flag = false;

        while (true)
        {
            boolean flag1 = false;

            synchronized (this.queueChunkUploads)
            {
                if (!this.queueChunkUploads.isEmpty())
                {
                    (this.queueChunkUploads.poll()).run();
                    flag1 = true;
                    flag = true;
                }
            }

            if (p_178516_1_ == 0L || !flag1)
            {
                break;
            }

//            long i = p_178516_1_ - System.nanoTime();

//            if (i < 0L)
//            {
//                break;
//            }
        }

        return flag;
    }
}
