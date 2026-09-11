package dev.ablinger.lightcraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(LightCraft.MOD_ID)
public class LightCraft {
  // Define mod id in a common place for everything to reference
  public static final String MOD_ID = "lightcraft";
  // Directly reference a slf4j logger
  public static final Logger LOGGER = LoggerFactory.getLogger(LightCraft.class);

  // Deferred Registers for Blocks and Items
  public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

  // Lapis Lux Ore Block with multiple variants
  public static final DeferredBlock<Block> LAPIS_LUX_ORE = BLOCKS.register("lapis_lux_ore", name -> {
    return new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)
        .setId(ResourceKey.create(Registries.BLOCK, name)));
  });

  // Raw Lapis Lux material block (for smelting)
  public static final DeferredBlock<Block> LAPIS_LUX_RAW_BLOCK = BLOCKS.register("raw_lapis_lux_block", name -> {
    return new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)
        .setId(ResourceKey.create(Registries.BLOCK, name)));
  });

  // Lapis Lux Ingot material block
  public static final DeferredBlock<Block> LAPIS_LUX_INGOT_BLOCK = BLOCKS.register("lapis_lux_ingot_block", name -> {
    return new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
        .setId(ResourceKey.create(Registries.BLOCK, name)));
  });

  // Item entries for the ore variants
  public static final DeferredItem<BlockItem> LAPIS_LUX_ORE_ITEM = ITEMS.register("lapis_lux_ore", name -> {
    return new BlockItem(LAPIS_LUX_ORE.get(), new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name)));
  });

  public static final DeferredItem<BlockItem> RAW_LAPIS_LUX_BLOCK_ITEM = ITEMS.register("raw_lapis_lux_block", name -> {
    return new BlockItem(LAPIS_LUX_RAW_BLOCK.get(), new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name)));
  });

  public static final DeferredItem<BlockItem> LAPIS_LUX_INGOT_BLOCK_ITEM = ITEMS.register("lapis_lux_ingot_block", name -> {
    return new BlockItem(LAPIS_LUX_INGOT_BLOCK.get(), new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name)));
  });

  // Standalone Raw Lapis Lux item (dropped from ore)
  public static final DeferredItem<Item> RAW_LAPIS_LUX_ITEM = ITEMS.register("raw_lapis_lux", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  // Standalone Lapis Lux ingot item
  public static final DeferredItem<Item> LAPIS_LUX_INGOT_ITEM = ITEMS.register("lapis_lux", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  // Register the commonSetup method for modloading
  public LightCraft(ModContainer container, IEventBus modEventBus) {
    container.registerConfig(ModConfig.Type.COMMON, Config.SPEC, MOD_ID);

    modEventBus.addListener(this::commonSetup);

    BLOCKS.register(modEventBus);
    ITEMS.register(modEventBus);

    NeoForge.EVENT_BUS.register(this);

    modEventBus.addListener(this::addCreative);
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    LOGGER.info("HELLO FROM COMMON SETUP");
    LOGGER.info("DIRT BLOCK >> {}", "minecraft:dirt");

    LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

    Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
  }

  // Add creative items to their respective tabs
  private void addCreative(BuildCreativeModeTabContentsEvent event) {
    // Add lapis lux blocks to building_blocks tab
    if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS) {
      event.accept(LAPIS_LUX_ORE_ITEM);
      event.accept(RAW_LAPIS_LUX_BLOCK_ITEM);
      event.accept(LAPIS_LUX_INGOT_BLOCK_ITEM);
    }
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    LOGGER.info("HELLO from server starting");
  }
}