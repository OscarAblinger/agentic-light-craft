package dev.ablinger.lightcraft;

import dev.ablinger.lightcraft.crop.LuxflowerBlock;
import dev.ablinger.lightcraft.crop.LuxflowerBlockEntity;
import dev.ablinger.lightcraft.crop.PetalBundles;
import dev.ablinger.lightcraft.data.ModData;
import dev.ablinger.lightcraft.gametest.LuxflowerGameTests;
import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.tooltip.TooltipLocation;
import net.neoforged.neoforge.event.RegisterTooltipAppendersEvent;
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

  // Deferred Registers for Blocks, Items, Block Entities and Data Components
  public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
  public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MOD_ID);
  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

  // Lapis Lux Ore Block with multiple variants
  // The ore keeps requiresCorrectToolForDrops (inherited from GOLD_ORE) so it
  // still needs a pickaxe, but strength 0.4F matches netherrack so it mines
  // quickly. It is NOT in the needs_iron_tool tag, so any pickaxe - wooden
  // included - can mine and harvest it.
  public static final DeferredBlock<Block> LAPIS_LUX_ORE = BLOCKS.register("lapis_lux_ore", name -> {
    return new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)
        .strength(0.4F) // mines as fast as netherrack
        .setId(ResourceKey.create(Registries.BLOCK, name)));
  });

  // Raw Lapis Lux material block (for smelting).
  //
  // Built from Properties.of() rather than copying GOLD_ORE/GOLD_BLOCK on
  // purpose: requiresCorrectToolForDrops() can only ever be SET to true, never
  // cleared, so copying a block that enables it would make the storage block
  // drop nothing when punched. Leaving it off means the block drops itself
  // even when mined by hand (just slower than with a pickaxe, which still
  // gets the mineable/pickaxe speed bonus from the generated tag).
  public static final DeferredBlock<Block> LAPIS_LUX_RAW_BLOCK = BLOCKS.register("raw_lapis_lux_block", name -> {
    return new Block(BlockBehaviour.Properties.of()
        .mapColor(MapColor.GOLD)
        .instrument(NoteBlockInstrument.BASEDRUM)
        .strength(0.4F) // mines as fast as netherrack; drops by hand
        .setId(ResourceKey.create(Registries.BLOCK, name)));
  });

  // Lapis Lux Ingot material block. Same hand-drop reasoning as the raw block
  // above; map color / instrument / sound mirror vanilla gold_block.
  public static final DeferredBlock<Block> LAPIS_LUX_INGOT_BLOCK = BLOCKS.register("lapis_lux_ingot_block", name -> {
    return new Block(BlockBehaviour.Properties.of()
        .mapColor(MapColor.GOLD)
        .instrument(NoteBlockInstrument.BELL)
        .sound(SoundType.METAL)
        .strength(0.4F) // mines as fast as netherrack; drops by hand
        .setId(ResourceKey.create(Registries.BLOCK, name)));
  });

  // Luxflower crop. It grows only in daylight under wheat's light requirement, is fully
  // grown after one minecraft day on average, and glows faintly from its third growth
  // stage on - twice as bright when fully grown (see LuxflowerBlock).
  public static final DeferredBlock<LuxflowerBlock> LUXFLOWER = BLOCKS.register("luxflower", name -> {
    return new LuxflowerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT)
        .setId(ResourceKey.create(Registries.BLOCK, name)));
  });

  // Item entries for the ore variants
  // NOTE: useBlockDescriptionPrefix() makes the BlockItem resolve its display name from
  // "block.lightcraft.<id>" instead of the Item default "item.lightcraft.<id>", matching
  // vanilla's Items.registerBlock(...). Without it the game shows the raw translation key.
  public static final DeferredItem<BlockItem> LAPIS_LUX_ORE_ITEM = ITEMS.register("lapis_lux_ore", name -> {
    return new BlockItem(LAPIS_LUX_ORE.get(), new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .useBlockDescriptionPrefix());
  });

  public static final DeferredItem<BlockItem> RAW_LAPIS_LUX_BLOCK_ITEM = ITEMS.register("raw_lapis_lux_block", name -> {
    return new BlockItem(LAPIS_LUX_RAW_BLOCK.get(), new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .useBlockDescriptionPrefix());
  });

  public static final DeferredItem<BlockItem> LAPIS_LUX_INGOT_BLOCK_ITEM = ITEMS.register("lapis_lux_ingot_block", name -> {
    return new BlockItem(LAPIS_LUX_INGOT_BLOCK.get(), new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .useBlockDescriptionPrefix());
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

  // Lapis Lux nugget (smallest unit; nine of these make an ingot)
  public static final DeferredItem<Item> LAPIS_LUX_NUGGET_ITEM = ITEMS.register("lapis_lux_nugget", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  // Luxflower seeds, doubles as the block item of the luxflower crop like vanilla wheat seeds
  public static final DeferredItem<BlockItem> LUXFLOWER_SEEDS = ITEMS.register("luxflower_seeds", name -> {
    return new BlockItem(LUXFLOWER.get(), new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .useItemDescriptionPrefix()
        .stacksTo(64));
  });

  // Luxflower petals, the day based harvest of a fully grown crop
  public static final DeferredItem<Item> LUXFLOWER_PETAL = ITEMS.register("luxflower_petal", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  // Petal bundles, compressing the petal harvest so big suncharges stay compact.
  // A small bundle holds 8 petals, each following tier multiplies that by 64.
  public static final DeferredItem<Item> SMALL_PETAL_BUNDLE = ITEMS.register("small_petal_bundle", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  public static final DeferredItem<Item> PETAL_BUNDLE = ITEMS.register("petal_bundle", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  public static final DeferredItem<Item> DOUBLE_PETAL_BUNDLE = ITEMS.register("double_petal_bundle", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  public static final DeferredItem<Item> TRIPLE_PETAL_BUNDLE = ITEMS.register("triple_petal_bundle", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  public static final DeferredItem<Item> QUADRUPLE_PETAL_BUNDLE = ITEMS.register("quadruple_petal_bundle", name -> {
    return new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, name))
        .stacksTo(64));
  });

  // Block entity of the luxflower, counting the daybreaks the crop witnessed
  public static final Supplier<BlockEntityType<LuxflowerBlockEntity>> LUXFLOWER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register("luxflower",
      () -> new BlockEntityType<>(LuxflowerBlockEntity::new, false, LUXFLOWER.get()));

  // Metadata of a luxflower seed: how many daybreaks the crop it came from witnessed
  public static final Supplier<DataComponentType<Integer>> SUNCHARGE = DATA_COMPONENT_TYPES.register("suncharge",
      () -> DataComponentType.<Integer>builder()
          .persistent(Codec.INT)
          .networkSynchronized(ByteBufCodecs.VAR_INT)
          .build());

  // Game tests of the seed dropping behaviour and glow, see gametest package
  public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, MOD_ID);

  public static final Supplier<Consumer<GameTestHelper>> LUXFLOWER_SEED_DROPS = TEST_FUNCTIONS.register("luxflower_seed_drops",
      () -> LuxflowerGameTests::luxflowerSeedDrops);

  public static final Supplier<Consumer<GameTestHelper>> LUXFLOWER_LIGHT_EMISSION = TEST_FUNCTIONS.register("luxflower_light_emission",
      () -> LuxflowerGameTests::luxflowerLightEmission);

  // The mod's own creative tab, holding every block and item of LightCraft
  public static final Supplier<CreativeModeTab> LIGHTCRAFT_TAB = CREATIVE_MODE_TABS.register("lightcraft",
      () -> CreativeModeTab.builder()
          .title(Component.translatable("itemGroup.lightcraft"))
          .icon(() -> new ItemStack(LUXFLOWER_PETAL.get()))
          .displayItems((parameters, output) -> {
            // Lapis lux materials and blocks
            output.accept(LAPIS_LUX_ORE_ITEM);
            output.accept(RAW_LAPIS_LUX_BLOCK_ITEM);
            output.accept(LAPIS_LUX_INGOT_BLOCK_ITEM);
            output.accept(RAW_LAPIS_LUX_ITEM);
            output.accept(LAPIS_LUX_INGOT_ITEM);
            output.accept(LAPIS_LUX_NUGGET_ITEM);
            // The luxflower crop and its harvest
            output.accept(LUXFLOWER_SEEDS);
            output.accept(LUXFLOWER_PETAL);
            output.accept(SMALL_PETAL_BUNDLE);
            output.accept(PETAL_BUNDLE);
            output.accept(DOUBLE_PETAL_BUNDLE);
            output.accept(TRIPLE_PETAL_BUNDLE);
            output.accept(QUADRUPLE_PETAL_BUNDLE);
          })
          .build());

  public LightCraft(ModContainer container, IEventBus modEventBus) {
    // No explicit name: the file becomes lightcraft-common.toml, already per-mod by itself.
    container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

    BLOCKS.register(modEventBus);
    ITEMS.register(modEventBus);
    BLOCK_ENTITY_TYPES.register(modEventBus);
    DATA_COMPONENT_TYPES.register(modEventBus);
    TEST_FUNCTIONS.register(modEventBus);
    CREATIVE_MODE_TABS.register(modEventBus);

    NeoForge.EVENT_BUS.register(this);

    modEventBus.addListener(this::registerTooltipAppenders);

    // Data generation: recipes, loot tables and block tags are produced by
    // the providers in dev.ablinger.lightcraft.data, not hand-authored. Both
    // event subtypes are registered because NeoForge has two datagen entry
    // points, each posting only one of them; see ModData for details.
    modEventBus.addListener(ModData::gatherDataClient);
    modEventBus.addListener(ModData::gatherDataServer);
  }

  // Show how many daybreaks a suncharged seed witnessed in its tooltip
  private void registerTooltipAppenders(RegisterTooltipAppendersEvent event) {
    event.registerComponentAppenderAfterAll(SUNCHARGE, (stack, context, display, player, flag, builder) -> {
      int suncharge = stack.getOrDefault(SUNCHARGE.get(), 0);
      if (suncharge > 0) {
        builder.accept(Component.translatable("lightcraft.tooltip.suncharge", group(suncharge)));
      }
    });

    // Show how many loose petals a petal bundle compresses
    event.registerAppender(TooltipLocation.POST_CUSTOM, (stack, context, display, player, flag, builder) -> {
      int petals = PetalBundles.petalsPer(stack.getItem());
      if (petals > 0) {
        builder.accept(Component.translatable("lightcraft.tooltip.petal_bundle", group(petals)));
      }
    });
  }

  // Formats a count with comma separators so big numbers stay readable (4,096 petals)
  private static String group(int value) {
    return String.format(Locale.ENGLISH, "%,d", value);
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    LOGGER.info("HELLO from server starting");
  }
}