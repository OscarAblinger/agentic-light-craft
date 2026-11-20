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
