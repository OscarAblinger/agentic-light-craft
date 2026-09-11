package dev.ablinger.lightcraft.tags;

import com.google.common.collect.ImmutableSet;
import dev.ablinger.lightcraft.LightCraft;

/**
 * Helper class for generating block tags for Lapis Lux items.
 */
public class BlockTagsGenerator {

    /**
     * Initialize block tags for lapis lux items.
     */
    public static void init(BlockTagsProvider provider) {
        // Tags will be handled by the provider's registration event
        // lapis_lux_ore should automatically be included in ores/gold tag
        // when registered via the DeferredRegister system
    }
}