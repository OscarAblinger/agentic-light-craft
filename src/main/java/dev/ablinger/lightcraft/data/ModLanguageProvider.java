package dev.ablinger.lightcraft.data;

import com.klikli_dev.modonomicon.api.datagen.AbstractModonomiconLanguageProvider;
import com.klikli_dev.modonomicon.api.datagen.ModonomiconLanguageProvider;
import dev.ablinger.lightcraft.LightCraft;
import net.minecraft.data.PackOutput;

/**
 * Generates {@code assets/lightcraft/lang/en_us.json}.
 *
 * <p>This provider is the single authority for LightCraft's English strings. Keeping
 * every translation here avoids a handwritten language file at the same output path
 * as Modonomicon's book language provider, which would let the generated file shadow
 * the handwritten one in the merged resource set.</p>
 *
 * <p>When Modonomicon book datagen is enabled, pass its shared
 * {@link com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache} to the
 * constructor that accepts a {@link ModonomiconLanguageProvider}. The book
 * translations are then merged into this file instead of a second provider writing
 * the same path.</p>
 */
public class ModLanguageProvider extends AbstractModonomiconLanguageProvider {

  public ModLanguageProvider(PackOutput output) {
    super(output, LightCraft.MOD_ID, "en_us");
  }

  public ModLanguageProvider(PackOutput output, ModonomiconLanguageProvider cachedProvider) {
    super(output, LightCraft.MOD_ID, "en_us", cachedProvider);
  }

  @Override
  protected void addTranslations() {
    // Creative tab
    this.add("itemGroup.lightcraft", "LightCraft");

    // Lapis Lux
    this.add("block.lightcraft.lapis_lux_ore", "Lapis Lux Ore");
    this.add("item.lightcraft.raw_lapis_lux", "Raw Lapis Lux");
    this.add("block.lightcraft.raw_lapis_lux_block", "Raw Lapis Lux Block");
    this.add("item.lightcraft.lapis_lux", "Lapis Lux Ingot");
    this.add("item.lightcraft.lapis_lux_nugget", "Lapis Lux Nugget");
    this.add("block.lightcraft.lapis_lux_ingot_block", "Lapis Lux Ingot Block");

    // Luxflower
    this.add("block.lightcraft.luxflower", "Luxflower");
    this.add("item.lightcraft.luxflower_seeds", "Luxflower Seeds");
    this.add("item.lightcraft.luxflower_petal", "Luxflower Petal");
    this.add("item.lightcraft.small_petal_bundle", "Small Petal Bundle");
    this.add("item.lightcraft.petal_bundle", "Petal Bundle");
    this.add("item.lightcraft.double_petal_bundle", "Double Petal Bundle");
    this.add("item.lightcraft.triple_petal_bundle", "Triple Petal Bundle");
    this.add("item.lightcraft.quadruple_petal_bundle", "Quadruple Petal Bundle");

    // In-game documentation
    this.add("item.lightcraft.light_book", "Light Book");

    // Pocket dimension book pages
    this.add("book.lightcraft.light_book.pocket_dimension.name", "Pocket Dimension");
    this.add("book.lightcraft.light_book.pocket_dimension.pocket_dimension.name", "Pocket Dimension");
    this.add("book.lightcraft.light_book.pocket_dimension.pocket_dimension.description", "Your private pocket cube.");
    this.add("book.lightcraft.light_book.pocket_dimension.pocket_dimension.overview.title", "The Pocket Dimension");
    this.add("book.lightcraft.light_book.pocket_dimension.pocket_dimension.overview.text",
        "Every player owns a private pocket cube: a small quartz room that is theirs alone. "
            + "The base pocket cube holds a 5x5x5 interior wrapped in a one-block-thick "
            + "indestructible quartz shell, for an outer cube of 7x7x7, and a bedrock-textured "
            + "exit block sits at the center of the floor, where the player always spawns. "
            + "All monsters are blocked from spawning in the pocket dimension, so the room "
            + "stays a safe, quiet place to build.");
    this.add("book.lightcraft.light_book.pocket_dimension.pocket_dimension.using.title", "Entering and Leaving");
    this.add("book.lightcraft.light_book.pocket_dimension.pocket_dimension.using.text",
        "Use the Pocket Dimension item to teleport into your own cube; the item is never "
            + "consumed. Use the item again - or right-click the bedrock exit block inside "
            + "the cube - to teleport back to where you entered, falling back to the respawn "
            + "point if that spot is no longer free.");
    this.add("book.lightcraft.light_book.pocket_dimension_growth.name", "Growing the Pocket Dimension");
    this.add("book.lightcraft.light_book.pocket_dimension_growth.pocket_dimension_growth.name", "Growing the Pocket Dimension");
    this.add("book.lightcraft.light_book.pocket_dimension_growth.pocket_dimension_growth.description", "How to grow your private pocket cube.");
    this.add("book.lightcraft.light_book.pocket_dimension_growth.pocket_dimension_growth.growth.title", "Growing Your Pocket Cube");
    this.add("book.lightcraft.light_book.pocket_dimension_growth.pocket_dimension_growth.growth.text",
        "While standing inside your pocket cube, use a Pocket Dimension Upgrade item to grow "
            + "it: the (7x7x7), (9x9x9), (11x11x11) or (15x15x15) upgrade expands the interior "
            + "accordingly. Upgrades are independent of each other, so the (15x15x15) upgrade "
            + "works straight from the base 5x5x5 cube. A successful upgrade consumes one "
            + "upgrade item - creative mode is exempt - and the new room keeps everything you "
            + "built. Using an upgrade for the same or a smaller size does nothing.");

    // Tooltips
    this.add("lightcraft.tooltip.suncharge", "Witnessed %s daybreaks");
    this.add("lightcraft.tooltip.petal_bundle", "%s petals");

    // Template examples (kept for compatibility with the previous handwritten file)
    this.add("block.lightcraft.example_block", "Example Block");
    this.add("item.lightcraft.example_item", "Example Item");

    // Config screens
    this.add("lightcraft.configuration.title", "LightCraft Configs");
    this.add("lightcraft.configuration.section.lightcraft.common.toml", "LightCraft Configs");
    this.add("lightcraft.configuration.section.lightcraft.common.toml.title", "LightCraft Configs");
  }

  @Override
  public String getName() {
    return "LightCraft English translations";
  }
}
