package me.nixuge.multithreadedchunkloading.replicas;

import lombok.Getter;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.EnumFacing;

import java.util.EnumSet;
import java.util.Set;

@Getter
public class ContainerLocalRenderInformation {
    final RenderChunk renderChunk;
    final EnumFacing facing;
    final Set<EnumFacing> setFacing;
    final int counter;

    public ContainerLocalRenderInformation(RenderChunk renderChunkIn, EnumFacing facingIn, int counterIn) {
        this.setFacing = EnumSet.noneOf(EnumFacing.class);
        this.renderChunk = renderChunkIn;
        this.facing = facingIn;
        this.counter = counterIn;
    }
}