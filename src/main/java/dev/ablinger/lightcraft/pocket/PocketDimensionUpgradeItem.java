package dev.ablinger.lightcraft.pocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The pocket dimension size upgrade items, one per size (7x7x7, 9x9x9,
 * 11x11x11, 15x15x15). Right-clicking one while standing inside your own cube
 * grows its interior to the item's size; the cube keeps everything you built
 * and one item is consumed for the upgrade.
 *
 * <p>The upgrades are independent of each other: a 15x15x15 upgrade works
 * straight from the base 5x5x5 cube. Using an item for the same size or a
 * smaller one than you already have does nothing and is not consumed, and
 * using one outside the pocket dimension does nothing either.
 */
public class PocketDimensionUpgradeItem extends Item {

  /** The interior edge length this item grows a cube to. */
  private final int size;

  public PocketDimensionUpgradeItem(Properties properties, int size) {
    super(properties);
    this.size = size;
  }

  /** {@return the interior edge length this item grows a cube to}. */
  public int size() {
    return this.size;
  }

  @Override
  public InteractionResult use(Level level, Player player, InteractionHand hand) {
    if (!PocketDimensions.isPocketDimension(level)) {
      // Upgrades only work inside the pocket dimension.
      return InteractionResult.PASS;
    }
    if (!(player instanceof ServerPlayer serverPlayer)) {
      // Client preview only; the server performs the actual upgrade.
      return InteractionResult.SUCCESS;
    }
    MinecraftServer server = level.getServer();
    int slot = PocketDimensions.slotFor(server, serverPlayer.getUUID());
    int current = PocketDimensions.sizeFor(server, serverPlayer.getUUID());
    // Same or smaller size: nothing happens, silently.
    if (this.size <= current) {
      return InteractionResult.PASS;
    }
    // Only while standing inside the player's own cube.
    if (!PocketDimensions.isInsideCube(slot, current, BlockPos.containing(serverPlayer.position()))) {
      return InteractionResult.PASS;
    }
    PocketDimensions.expandCube(server.getLevel(PocketDimensions.DIMENSION), serverPlayer.getUUID(), this.size);
    // The upgrade happened: one item is paid for it. Only the successful path
    // consumes; refused upgrades (same/smaller size) keep the item. Creative
    // mode keeps its items, like every other consumable in vanilla.
    if (!serverPlayer.getAbilities().instabuild) {
      player.getItemInHand(hand).shrink(1);
    }
    serverPlayer.sendSystemMessage(
        Component.translatable("lightcraft.pocket.expanded", String.valueOf(this.size)), true);
    return InteractionResult.SUCCESS_SERVER;
  }
}
