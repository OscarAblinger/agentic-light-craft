package dev.ablinger.lightcraft.data;

import java.util.concurrent.CompletableFuture;

import dev.ablinger.lightcraft.LightCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;

/**
 * Block tags for the lapis lux blocks.
 *
 * The ore sets {@code requiresCorrectToolForDrops} (inherited from gold ore),
 * so it yields nothing unless the breaking tool matches a
 * {@code mineable/<tool>} tag - hence {@code mineable/pickaxe}. It is
 * deliberately NOT added to any {@code needs_*_tool} tier tag, so a plain
 * wooden pickaxe is enough to mine and harvest it.
 *
 * The two storage blocks do not set {@code requiresCorrectToolForDrops} at
 * all (see LightCraft's block properties), so they drop themselves even when
 * punched by hand. The pickaxe tag is still applied to them so that using a
 * pickaxe gets the normal mining-speed bonus rather than hand speed.
 */
public class ModBlockTagsProvider extends BlockTagsProvider {

  public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
    super(output, lookupProvider, LightCraft.MOD_ID);
  }

  @Override
  protected void addTags(HolderLookup.Provider provider) {
    this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
        .add(
            LightCraft.LAPIS_LUX_ORE.get(),
            LightCraft.LAPIS_LUX_RAW_BLOCK.get(),
            LightCraft.LAPIS_LUX_INGOT_BLOCK.get());

    // No needs_stone_tool / needs_iron_tool entry: the ore is intentionally
    // harvestable with a wooden pickaxe.
  }

  @Override
  public String getName() {
    return "LightCraft Block Tags";
  }
}
