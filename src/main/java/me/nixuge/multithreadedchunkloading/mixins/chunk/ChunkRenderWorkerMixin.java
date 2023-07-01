package me.nixuge.multithreadedchunkloading.mixins.chunk;

import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRenderWorker.class)
public class ChunkRenderWorkerMixin {
    //@Inject(method = "processTask", at = @At("HEAD"))
    //protected void processTask(ChunkCompileTaskGenerator generator, CallbackInfo ci) throws InterruptedException {
    //    throw new InterruptedException();
    //}
//    @Inject(method = "run" ,at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;getNextChunkUpdate()Lnet/minecraft/client/renderer/chunk/ChunkCompileTaskGenerator;"))
//    public void processTaskLock(CallbackInfo ci) {
//        System.out.println("invoke: " + java.time.LocalTime.now());
//    }
//    @Inject(method = "run" ,at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;getNextChunkUpdate()Lnet/minecraft/client/renderer/chunk/ChunkCompileTaskGenerator;"))
//    public void processTaskUnlock(CallbackInfo ci) {
//        System.out.println("after : " + java.time.LocalTime.now());
//    }


}