package dev.ablinger.lightcraft.pocket;

import dev.ablinger.lightcraft.LightCraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Event handlers guarding the pocket dimension.
 *
 * <ul>
 *   <li>Per player tick: anyone inside the pocket dimension who is not within
 *       their 5x5x5 cube is teleported back in and the cube is regenerated,
 *       see {@link PocketDimensions#ensureInsideCube}.</li>
 *   <li>Block break and explosion: the quartz walls and the exit block are
 *       indestructible. Hardness -1 already stops survival mining; these
 *       handlers also cover creative breaking and explosions.</li>
 *   <li>Entity join: no monster may exist in the pocket dimension. Natural
 *       spawns, spawn eggs and even a spawned Wither are blocked, which keeps
 *       the cube a safe room and the walls out of reach of mob griefing.</li>
 * </ul>
 *
 * Registered as an instance on the {@code NeoForge.EVENT_BUS} from
 * {@link LightCraft}'s constructor.
 */
public class PocketDimensionEvents {

  @SubscribeEvent
  public void onPlayerTick(PlayerTickEvent.Post event) {
    Player player = event.getEntity();
    // The tick event fires on both logical sides; only the server moves players.
    if (player.level().isClientSide()) {
      return;
    }
    if (player instanceof ServerPlayer serverPlayer && PocketDimensions.isPocketDimension(player.level())) {
      PocketDimensions.ensureInsideCube(serverPlayer);
    }
  }

  @SubscribeEvent
  public void onBlockBreak(BreakBlockEvent event) {
    // Cancelling covers every mining path, creative mode included; the -1
    // hardness only ever stops survival mining.
    if (isPocketBlock(event.getState())) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onExplosionDetonate(ExplosionEvent.Detonate event) {
    // The huge explosion resistance already makes the blocks immune; removing
    // them from the affected list is the hard guarantee.
    event.getAffectedBlocks().removeIf(pos -> isPocketBlock(event.getLevel().getBlockState(pos)));
  }

  @SubscribeEvent
  public void onEntityJoin(EntityJoinLevelEvent event) {
    // Fires on both logical sides and also when chunks load; only naturally
    // added server-side monsters are blocked, existing saved entities still load.
    if (event.getLevel().isClientSide() || event.loadedFromDisk()) {
      return;
    }
    if (PocketDimensions.isPocketDimension(event.getLevel()) && event.getEntity() instanceof Monster) {
      event.setCanceled(true);
    }
  }

  private static boolean isPocketBlock(BlockState state) {
    return state.is(LightCraft.POCKET_DIMENSION_QUARTZ.get())
        || state.is(LightCraft.POCKET_DIMENSION_EXIT.get());
  }
}
