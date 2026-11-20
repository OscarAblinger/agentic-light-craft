package dev.ablinger.lightcraft.data;

import com.klikli_dev.modonomicon.api.datagen.LanguageProviderCache;
import com.klikli_dev.modonomicon.api.datagen.NeoBookProvider;
import com.klikli_dev.modonomicon.api.datagen.NeoResearchProvider;
import com.klikli_dev.modonomicon.api.datagen.research.ResearchCache;
import dev.ablinger.lightcraft.data.book.LightBook;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * Entry point for LightCraft's data generation.
 *
 * Registered on the mod event bus from {@code LightCraft}'s constructor via
 * {@code modEventBus.addListener(...)}, matching how this project already wires
 * {@code commonSetup} and {@code registerTooltipAppenders}.
 *
 * 26.x split the old single {@code GatherDataEvent} (which had
 * includeServer()/includeClient() predicates) into two subtypes, and there are
 * two separate datagen entry points that each post only one of them:
 * {@code net.minecraft.data.Main} posts {@link GatherDataEvent.Server}, while
 * {@code net.minecraft.client.data.Main} posts {@link GatherDataEvent.Client}.
 * This project's {@code data} run uses {@code clientData()}, i.e. the client
 * entry point, so only the Client subtype ever fires here. The event bus also
 * refuses listeners on the abstract base type, so both concrete subtypes are
 * registered and delegate to the same helper. Exactly one subtype is posted per
 * run, so the providers are added once regardless of which entry point is used.
 *
 * Recipes go through {@link ModRecipeProvider.Runner} rather than the provider
 * itself - RecipeProvider.Runner owns the PackOutput/registry-future pair and
 * only builds the provider once the lookups resolve.
 */
public final class ModData {

  private ModData() {}

  public static void gatherDataClient(GatherDataEvent.Client event) {
    addProviders(event);
  }

  public static void gatherDataServer(GatherDataEvent.Server event) {
    addProviders(event);
  }

  private static void addProviders(GatherDataEvent event) {
    var packOutput = event.getGenerator().getPackOutput();

    event.addProvider(new ModRecipeProvider.Runner(
        packOutput,
        event.getLookupProvider()));

    event.addProvider(new ModLootTableProvider(
        packOutput,
        event.getLookupProvider()));

    event.addProvider(new ModBlockTagsProvider(
        packOutput,
        event.getLookupProvider()));

    // The book and language providers share a cache so book translations are
    // merged into the single assets/lightcraft/lang/en_us.json file. Modonomicon
    // also requires a research provider, even when no research is authored.
    var langCache = new LanguageProviderCache("en_us");
    var researchCache = new ResearchCache();

    event.addProvider(NeoBookProvider.of(event, langCache, researchCache,
        new LightBook()));
    event.addProvider(NeoResearchProvider.of(event, langCache, researchCache));

    // Keep this provider last: it owns assets/lightcraft/lang/en_us.json, so it
    // must run after the book provider has filled the shared language cache.
    event.addProvider(new ModLanguageProvider(packOutput, langCache));
  }
}
