package me.nixuge.multithreadedchunkloading.mixins;

import com.google.common.collect.Lists;
import me.nixuge.multithreadedchunkloading.mixins.accessor.ViewFrustumAccessor;
import me.nixuge.multithreadedchunkloading.replicas.ContainerLocalRenderInformation;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vector3d;
import org.lwjgl.util.vector.Vector3f;
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
    @Shadow
    private double frustumUpdatePosX;
    @Shadow
    private double frustumUpdatePosY;
    @Shadow
    private double frustumUpdatePosZ;
    @Shadow
    private int frustumUpdatePosChunkX;
    @Shadow
    private int frustumUpdatePosChunkY;
    @Shadow
    private int frustumUpdatePosChunkZ;


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

//    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/BlockPos;distanceSq(DDD)D"), remap = false)
//    public double owo(double toX, double toY, double toZ) {
//        System.out.println("CALLED THINGY");
//        return 1000;
//    }


    // ===== Failed attempt at fixing optifine below =====
    // Basically trying to replicate Vanilla behavior, but for some reason it just doesn't work.
    // Only problem left with optifine is that it tries to optimize the chunk loading, and so
    // makes it slower on purpose. This is of course the exact contrary of what we want, and is
    // REALLY annoying to change.
    //
    // I've basically spent ~3 hours trying to get this to work, basically "ripping" the vanilla mechanic
    // & reimplementing it.
    // Problem is: it's not working. Even if I just copy paste, for some reason it just doesn't.
    // Tried remaking it a bit adapted here. Problems are:
    // - (camera).isBoundingBoxInFrustum(renderchunk1.boundingBox) is for some reason almost ALWAYS false
    // - even if I disable that check, everything is just empty, which is weird considering it's basically
    // the same as vanilla
    //
    // TODO: try to rip up the (literally) ENTIRE thing, see if it works.
    // if it doesn't, give up.
    // if it does, work from there to redo it
    //
    // Note that even tho i'm spending hours on this, it's by no means necessary.
    // The mod is still VERY effective with Optifine, a bit slower but pretty good overall.
    // Honestly for now leaving it like that. Will see if I have some time to spare at some
    // point in the future.
    // For now, it's no need.

    @Shadow
    private int renderDistanceChunks;
    @Shadow
    private ViewFrustum viewFrustum;
    @Shadow
    private ChunkRenderContainer renderContainer;
    @Shadow
    private ClippingHelper debugFixedClippingHelper;
    @Shadow
    @Final
    private Vector3d debugTerrainFrustumPosition;

    // Fuck optifine
    // This function is completely removed (replaced with other arguments)
    // and the mappings are fucked while at it.
    private RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing) {
        BlockPos blockpos = renderChunkBase.getBlockPosOffset16(facing);
        return MathHelper.abs_int(playerPos.getX() - blockpos.getX()) > this.renderDistanceChunks * 16 ? null : (blockpos.getY() >= 0 && blockpos.getY() < 256 ? (MathHelper.abs_int(playerPos.getZ() - blockpos.getZ()) > this.renderDistanceChunks * 16 ? null : ((ViewFrustumAccessor) this.viewFrustum).invokeGetRenderChunk(blockpos)) : null);
    }

    @Shadow
    private boolean isPositionInRenderChunk(BlockPos pos, RenderChunk renderChunkIn) {
        return false;
    }


    private final List<ContainerLocalRenderInformation> newRenderInfosFuck = Lists.newArrayListWithCapacity(69696);


//    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/Set;add(Ljava/lang/Object;)Z"))
//    public boolean owo(Set<Object> instance, Object e) {
////        System.out.println("CALLED ! " + e.getClass());
//        if (instance.equals(chunksToUpdate)) {
////            System.out.println("WE GOT IT LESS GOOO");
//            return false;
//        }
//        return instance.add(e);
////        return false;
//    }

    public void addShortCircuit(RenderChunk renderchunk4) {
//        System.out.println("short circuited lmfao");
//        this.chunksToUpdate.add(renderchunk4);
        List<RenderChunk> lmao = new ArrayList<>();
        lmao.add(renderchunk4);
        this.chunksToUpdate.addAll(lmao);
//        System.out.println("done short circuiting");
    }

    Set<RenderChunk> set;

    @Shadow
    private Set<EnumFacing> getVisibleFacings(BlockPos pos){return null;}
    @Shadow
    protected Vector3f getViewVector(Entity entityIn, double partialTicks) {return null;}

//    @Inject(method = "setupTerrain", at = @At("HEAD"))
//    public void atHead(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
//        set = this.chunksToUpdate;
//    }

    @Inject(method = "setupTerrain", at = @At("RETURN"))
    public void please(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        set = this.chunksToUpdate;
        double d0 = viewEntity.posX - this.frustumUpdatePosX;
        double d1 = viewEntity.posY - this.frustumUpdatePosY;
        double d2 = viewEntity.posZ - this.frustumUpdatePosZ;

        if (this.frustumUpdatePosChunkX != viewEntity.chunkCoordX || this.frustumUpdatePosChunkY != viewEntity.chunkCoordY || this.frustumUpdatePosChunkZ != viewEntity.chunkCoordZ || d0 * d0 + d1 * d1 + d2 * d2 > 16.0D)
        {
            this.frustumUpdatePosX = viewEntity.posX;
            this.frustumUpdatePosY = viewEntity.posY;
            this.frustumUpdatePosZ = viewEntity.posZ;
            this.frustumUpdatePosChunkX = viewEntity.chunkCoordX;
            this.frustumUpdatePosChunkY = viewEntity.chunkCoordY;
            this.frustumUpdatePosChunkZ = viewEntity.chunkCoordZ;
            this.viewFrustum.updateChunkPositions(viewEntity.posX, viewEntity.posZ);
        }


        double d3 = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * partialTicks;
        double d4 = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * partialTicks;
        double d5 = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * partialTicks;

        this.renderContainer.initialize(d3, d4, d5);

        if (this.debugFixedClippingHelper != null)
        {
            Frustum frustum = new Frustum(this.debugFixedClippingHelper);
            frustum.setPosition(this.debugTerrainFrustumPosition.x, this.debugTerrainFrustumPosition.y, this.debugTerrainFrustumPosition.z);
            camera = frustum;
        }

        BlockPos blockpos1 = new BlockPos(d3, d4 + (double) viewEntity.getEyeHeight(), d5);
        RenderChunk renderchunk = ((ViewFrustumAccessor) this.viewFrustum).invokeGetRenderChunk(blockpos1);
        BlockPos blockpos = new BlockPos(MathHelper.floor_double(d3 / 16.0D) * 16, MathHelper.floor_double(d4 / 16.0D) * 16, MathHelper.floor_double(d5 / 16.0D) * 16);
        Queue<ContainerLocalRenderInformation> queue = Lists.newLinkedList();
        {
            int i = blockpos1.getY() > 0 ? 248 : 8;

            for (int j = -this.renderDistanceChunks; j <= this.renderDistanceChunks; ++j) {
                for (int k = -this.renderDistanceChunks; k <= this.renderDistanceChunks; ++k) {
                    RenderChunk renderchunk1 = ((ViewFrustumAccessor) this.viewFrustum).invokeGetRenderChunk(new BlockPos((j << 4) + 8, i, (k << 4) + 8));
//                    System.out.println((camera).isBoundingBoxInFrustum(renderchunk1.boundingBox));
                    if (renderchunk1 != null && (camera).isBoundingBoxInFrustum(renderchunk1.boundingBox)) {
                        renderchunk1.setFrameIndex(frameCount);
//                        System.out.println("added!!!!!!!!");
                        queue.add(new ContainerLocalRenderInformation(renderchunk1, null, 0));
                    }
                }
            }
        }
        this.newRenderInfosFuck.clear();

        boolean flag2 = false;
        ContainerLocalRenderInformation containerlri3 = new ContainerLocalRenderInformation(renderchunk, null, 0);
        Set<EnumFacing> set1 = this.getVisibleFacings(blockpos1);

        if (set1.size() == 1)
        {
            Vector3f vector3f = this.getViewVector(viewEntity, partialTicks);
            EnumFacing enumfacing = EnumFacing.getFacingFromVector(vector3f.x, vector3f.y, vector3f.z).getOpposite();
            set1.remove(enumfacing);
        }

        if (set1.isEmpty())
        {
            flag2 = true;
        }

        if (flag2 && !playerSpectator)
        {
            this.newRenderInfosFuck.add(containerlri3);
        }
        else
        {
            renderchunk.setFrameIndex(frameCount);
            queue.add(containerlri3);
        }

        while (!(queue).isEmpty()) {
            ContainerLocalRenderInformation container1 = queue.poll();
            RenderChunk renderchunk3 = container1.getRenderChunk();
            EnumFacing enumfacing2 = container1.getFacing();
            this.newRenderInfosFuck.add(container1);

            for (EnumFacing enumfacing1 : EnumFacing.values()) {
                RenderChunk renderchunk2 = this.getRenderChunkOffset(blockpos, renderchunk3, enumfacing1);

                if ((!container1.getSetFacing().contains(enumfacing1.getOpposite())) && (enumfacing2 == null || renderchunk3.getCompiledChunk().isVisible(enumfacing2.getOpposite(), enumfacing1)) && renderchunk2 != null && renderchunk2.setFrameIndex(frameCount) && (camera).isBoundingBoxInFrustum(renderchunk2.boundingBox)) {
                    ContainerLocalRenderInformation container0 = new ContainerLocalRenderInformation(renderchunk2, enumfacing1, container1.getCounter() + 1);
                    container0.getSetFacing().addAll(container1.getSetFacing());
                    container0.getSetFacing().add(enumfacing1);
                    queue.add(container0);
                }
            }
        }
        List<RenderChunk> lmao = new ArrayList<>();
        for (ContainerLocalRenderInformation container2 : this.newRenderInfosFuck) {
            RenderChunk renderchunk4 = container2.getRenderChunk();

            if ((renderchunk4.isNeedsUpdate() || set.contains(renderchunk4))) {

                if (this.isPositionInRenderChunk(blockpos, container2.getRenderChunk())) {
                    this.renderDispatcher.updateChunkNow(renderchunk4);
                    renderchunk4.setNeedsUpdate(false);
                } else {
                    System.out.println("added?");
//                    addShortCircuit(renderchunk4);
                    lmao.add(renderchunk4);
                }
            }
        }
        this.chunksToUpdate.addAll(lmao);


    }
}

//    boolean flag4 = blockpos.distanceSq((double)(blockpos1.getX() + 8), (double)(blockpos1.getY() + 8), (double)(blockpos1.getZ() + 8)) < 768.0D;