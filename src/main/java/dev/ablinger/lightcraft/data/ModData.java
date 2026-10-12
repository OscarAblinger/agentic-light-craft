package dev.ablinger.lightcraft.data;

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
    event.addProvider(new ModRecipeProvider.Runner(
        event.getGenerator().getPackOutput(),
        event.getLookupProvider()));

    event.addProvider(new ModLootTableProvider(
        event.getGenerator().getPackOutput(),
        event.getLookupProvider()));

    event.addProvider(new ModBlockTagsProvider(
        event.getGenerator().getPackOutput(),
        event.getLookupProvider()));
  }
}
