package dev.ablinger.lightcraft.pocket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The Pocket Dimension artifact.
 *
 * <p>Using it teleports the holder into their own private pocket cube (or back
 * out again when used inside one), see {@link PocketDimensions}. The item is
 * per-player, not per-item: it never teleports anyone but its holder, the cube
 * it leads to belongs to the holder no matter how many of these items exist,
 * and using it never consumes or changes the item.
 */
public class PocketDimensionItem extends Item {

  public PocketDimensionItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (!(player instanceof ServerPlayer serverPlayer)) {
      // Client preview only; the server performs the actual teleport.
      return InteractionResult.SUCCESS;
    }
    if (PocketDimensions.isPocketDimension(level)) {
      PocketDimensions.exit(serverPlayer);
    } else {
      PocketDimensions.enter(serverPlayer);
    }
    return InteractionResult.SUCCESS_SERVER;
  }
}
