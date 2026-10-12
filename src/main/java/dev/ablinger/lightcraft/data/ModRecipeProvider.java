package dev.ablinger.lightcraft.data;

import java.util.concurrent.CompletableFuture;

import dev.ablinger.lightcraft.LightCraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Crafting and cooking recipes for the full lapis lux material chain, mirroring
 * vanilla iron/gold: 3x3 storage blocks in both raw and ingot form, nuggets at
 * 9-per-ingot, and smelting/blasting from both the ore and the raw chunk.
 *
 * Every recipe is keyed explicitly rather than relying on builder id
 * derivation, because ids would otherwise be derived from the RESULT - and
 * "ingot from nuggets" and "ingot from storage block" would collide.
 *
 * The entry point is {@link Runner}, not this class: 26.x drives recipes
 * through {@link RecipeProvider.Runner}, which owns the PackOutput/future pair
 * and constructs a provider only once the registry lookups have resolved.
 */
public class ModRecipeProvider extends RecipeProvider {

  public ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
    super(registries, output);
  }

  private static ResourceKey<Recipe<?>> key(String name) {
    return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(LightCraft.MOD_ID, name));
  }

  @Override
  protected void buildRecipes() {
    // --- storage blocks: 9 units in, 1 block out ---------------------------
    this.shaped(RecipeCategory.BUILDING_BLOCKS, LightCraft.LAPIS_LUX_INGOT_BLOCK_ITEM.get())
        .pattern("###")
        .pattern("###")
        .pattern("###")
        .define('#', LightCraft.LAPIS_LUX_INGOT_ITEM.get())
        .unlockedBy("has_lapis_lux_ingot", has(LightCraft.LAPIS_LUX_INGOT_ITEM.get()))
        .save(this.output, key("lapis_lux_ingot_block"));

    this.shaped(RecipeCategory.BUILDING_BLOCKS, LightCraft.RAW_LAPIS_LUX_BLOCK_ITEM.get())
        .pattern("###")
        .pattern("###")
        .pattern("###")
        .define('#', LightCraft.RAW_LAPIS_LUX_ITEM.get())
        .unlockedBy("has_raw_lapis_lux", has(LightCraft.RAW_LAPIS_LUX_ITEM.get()))
        .save(this.output, key("raw_lapis_lux_block"));

    // --- unpacking: 1 block back out as 9 ----------------------------------
    this.shapeless(RecipeCategory.MISC, LightCraft.LAPIS_LUX_INGOT_ITEM.get(), 9)
        .requires(LightCraft.LAPIS_LUX_INGOT_BLOCK_ITEM.get())
        .unlockedBy("has_lapis_lux_ingot_block", has(LightCraft.LAPIS_LUX_INGOT_BLOCK_ITEM.get()))
        .save(this.output, key("lapis_lux_ingot_from_ingot_block"));

    this.shapeless(RecipeCategory.MISC, LightCraft.RAW_LAPIS_LUX_ITEM.get(), 9)
        .requires(LightCraft.RAW_LAPIS_LUX_BLOCK_ITEM.get())
        .unlockedBy("has_raw_lapis_lux_block", has(LightCraft.RAW_LAPIS_LUX_BLOCK_ITEM.get()))
        .save(this.output, key("raw_lapis_lux_from_raw_block"));

    // --- nuggets: 9 nuggets <-> 1 ingot ------------------------------------
    this.shaped(RecipeCategory.MISC, LightCraft.LAPIS_LUX_INGOT_ITEM.get())
        .pattern("###")
        .pattern("###")
        .pattern("###")
        .define('#', LightCraft.LAPIS_LUX_NUGGET_ITEM.get())
        .unlockedBy("has_lapis_lux_nugget", has(LightCraft.LAPIS_LUX_NUGGET_ITEM.get()))
        .save(this.output, key("lapis_lux_from_nuggets"));

    this.shapeless(RecipeCategory.MISC, LightCraft.LAPIS_LUX_NUGGET_ITEM.get(), 9)
        .requires(LightCraft.LAPIS_LUX_INGOT_ITEM.get())
        .unlockedBy("has_lapis_lux_ingot", has(LightCraft.LAPIS_LUX_INGOT_ITEM.get()))
        .save(this.output, key("lapis_lux_nugget_from_ingot"));

    // --- cooking: ore and raw chunk both smelt to the ingot ----------------
    // Smelting at iron/gold's 0.7 XP; blasting at half the cook time, matching
    // vanilla's 200/100 tick pair.
    SimpleCookingRecipeBuilder.smelting(
            Ingredient.of(LightCraft.RAW_LAPIS_LUX_ITEM.get()),
            RecipeCategory.MISC,
            CookingBookCategory.MISC,
            LightCraft.LAPIS_LUX_INGOT_ITEM.get(),
            0.7F,
            200)
        .unlockedBy("has_raw_lapis_lux", has(LightCraft.RAW_LAPIS_LUX_ITEM.get()))
        .group("lapis_lux")
        .save(this.output, key("lapis_lux_from_smelting_raw_lapis_lux"));

    SimpleCookingRecipeBuilder.blasting(
            Ingredient.of(LightCraft.RAW_LAPIS_LUX_ITEM.get()),
            RecipeCategory.MISC,
            CookingBookCategory.MISC,
            LightCraft.LAPIS_LUX_INGOT_ITEM.get(),
            0.7F,
            100)
        .unlockedBy("has_raw_lapis_lux", has(LightCraft.RAW_LAPIS_LUX_ITEM.get()))
        .group("lapis_lux")
        .save(this.output, key("lapis_lux_from_blasting_raw_lapis_lux"));

    SimpleCookingRecipeBuilder.smelting(
            Ingredient.of(LightCraft.LAPIS_LUX_ORE_ITEM.get()),
            RecipeCategory.MISC,
            CookingBookCategory.MISC,
            LightCraft.LAPIS_LUX_INGOT_ITEM.get(),
            0.7F,
            200)
        .unlockedBy("has_lapis_lux_ore", has(LightCraft.LAPIS_LUX_ORE_ITEM.get()))
        .group("lapis_lux")
        .save(this.output, key("lapis_lux_from_smelting_lapis_lux_ore"));

    SimpleCookingRecipeBuilder.blasting(
            Ingredient.of(LightCraft.LAPIS_LUX_ORE_ITEM.get()),
            RecipeCategory.MISC,
            CookingBookCategory.MISC,
            LightCraft.LAPIS_LUX_INGOT_ITEM.get(),
            0.7F,
            100)
        .unlockedBy("has_lapis_lux_ore", has(LightCraft.LAPIS_LUX_ORE_ITEM.get()))
        .group("lapis_lux")
        .save(this.output, key("lapis_lux_from_blasting_lapis_lux_ore"));
  }

  /** Hands the data generator a provider once registry lookups are ready. */
  public static class Runner extends RecipeProvider.Runner {
    public Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
      super(packOutput, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(
        HolderLookup.Provider registries, RecipeOutput output) {
      return new ModRecipeProvider(registries, output);
    }

    @Override
    public String getName() {
      return "LightCraft Recipes";
    }
  }
}
