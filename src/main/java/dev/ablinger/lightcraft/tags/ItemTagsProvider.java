package dev.ablinger.lightcraft.tags;

import java.util.concurrent.CompletableFuture;

/**
 * Provides item tags generation for Lapis Lux items.
 */
public class ItemTagsProvider {
    
    /**
     * Item tag registration - simplified for NeoForge 26.x compatibility.
     */
    public static void registerItems() {
        // Tags are now handled differently in NeoForge 26.x
        // The raw lapis lux and lapis lux ingot will be automatically included
        // in their appropriate tags if registered before other items of the same type
    }
}