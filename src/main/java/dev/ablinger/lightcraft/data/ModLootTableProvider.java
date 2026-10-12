package dev.ablinger.lightcraft.data;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import dev.ablinger.lightcraft.LightCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Loot tables for the three lapis lux blocks.
 *
 * The ore is the interesting one: it drops raw lapis lux at roughly half the
 * rate vanilla lapis ore drops lapis lazuli (uniform 2-4 against vanilla's
 * uniform 4-9), keeping lapis lux the rarer material. Silk touch yields the
 * ore block itself, and Fortune scales the drop count via vanilla's
 * {@code ore_drops} formula, exactly as it does for every other ore.
 */
public class ModLootTableProvider extends LootTableProvider {

  public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
    super(
        output,
        Set.of(),
        List.of(new SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)),
        registries);
  }

  // getName() is final on LootTableProvider ("Loot Tables"), so it is not
  // overridden here.

  private static class ModBlockLoot extends BlockLootSubProvider {

    protected ModBlockLoot(HolderLookup.Provider registries) {
      super(Set.<Item>of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
      HolderLookup.RegistryLookup<Enchantment> enchantments =
          this.registries.lookupOrThrow(Registries.ENCHANTMENT);

      // Ore: silk touch -> the block, otherwise 2-4 raw lapis lux with
      // Fortune support and normal explosion scatter. Modelled directly on
      // vanilla's createLapisOreDrops, with the count range halved.
      this.add(
          LightCraft.LAPIS_LUX_ORE.get(),
          (Block block) -> this.createSilkTouchDispatchTable(
              block,
              this.applyExplosionDecay(
                  block,
                  LootItem.lootTableItem(LightCraft.RAW_LAPIS_LUX_ITEM.get())
                      .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                      .apply(ApplyBonusCount.addOreBonusCount(
                          enchantments.getOrThrow(Enchantments.FORTUNE))))));

      // Storage blocks drop themselves, like vanilla's gold/iron blocks.
      this.dropSelf(LightCraft.LAPIS_LUX_RAW_BLOCK.get());
      this.dropSelf(LightCraft.LAPIS_LUX_INGOT_BLOCK.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
      return List.of(
          LightCraft.LAPIS_LUX_ORE.get(),
          LightCraft.LAPIS_LUX_RAW_BLOCK.get(),
          LightCraft.LAPIS_LUX_INGOT_BLOCK.get());
    }
  }
}
