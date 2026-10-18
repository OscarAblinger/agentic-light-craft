package dev.ablinger.lightcraft.pocket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The bedrock-looking block at the center of every pocket cube's floor - the
 * permanent way out. Right-clicking it teleports the player back to where they
 * stood when they entered (or to their respawn point), exactly like using the
 * pocket dimension item again. It shares the vanilla bedrock texture and, like
 * the quartz walls, it can never be broken.
 */
public class PocketDimensionExitBlock extends Block {

  public PocketDimensionExitBlock(Properties properties) {
    super(properties);
  }

  @Override
  protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
      BlockHitResult hitResult) {
    if (!(player instanceof ServerPlayer serverPlayer)) {
      // Client preview only; the server performs the actual teleport.
      return InteractionResult.SUCCESS;
    }
    PocketDimensions.exit(serverPlayer);
    return InteractionResult.SUCCESS_SERVER;
  }
}
