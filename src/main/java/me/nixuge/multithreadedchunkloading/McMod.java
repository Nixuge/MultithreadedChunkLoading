package me.nixuge.multithreadedchunkloading;

import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(
        modid = McMod.MOD_ID,
        name = McMod.NAME,
        version = McMod.VERSION,
        clientSideOnly = true
)

@Setter
public class McMod {
    public static final String MOD_ID = "multithreadedchunkloading";
    public static final String NAME = "MultithreadedChunkLoading";
    public static final String VERSION = "1.0.0";


    @Getter
    @Mod.Instance(value = McMod.MOD_ID)
    private static McMod instance;

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        // Setup your config here
    }

    @Mod.EventHandler
    public void init(final FMLInitializationEvent event) {
        // Setup your events here & register your commands here
        //
        // MinecraftForge.EVENT_BUS.register(...);
        //
        // ClientCommandHandler.instance.registerCommand(...);
    }
}
