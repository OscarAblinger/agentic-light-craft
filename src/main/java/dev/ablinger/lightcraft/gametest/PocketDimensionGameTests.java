package dev.ablinger.lightcraft.gametest;

import dev.ablinger.lightcraft.LightCraft;
import dev.ablinger.lightcraft.pocket.PocketDimensionData;
import dev.ablinger.lightcraft.pocket.PocketDimensions;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Game tests for the pocket dimension.
 *
 * <p>The cube logic is written against a slot rather than a player, so the
 * geometry and regeneration can be tested without moving a real player across
 * dimensions: the tests build a cube far out in the actual pocket dimension,
 * damage it, regenerate it and verify that walls, interior, exit block and the
 * cleanup of stray outside blocks all behave.
 */
public final class PocketDimensionGameTests {

  /**
   * The slot under test. Real players start at slot 0 and only climb upwards,
   * so a high slot never collides with an actual player's cube.
   */
  private static final int TEST_SLOT = 100;

  private PocketDimensionGameTests() {
  }

  /**
   * Builds the cube of a fresh slot, checks its shape (quartz shell, 5x5x5 air
   * interior, exit block at the floor's center), then damages it - a hole in
   * the wall and stray blocks just outside the cube - and regenerates,
   * verifying the shell and strays are restored/destroyed while a block the
   * player built inside the interior survives.
   */
  public static void pocketCubeRegeneration(GameTestHelper helper) {
    MinecraftServer server = helper.getLevel().getServer();
    ServerLevel pocket = server.getLevel(PocketDimensions.DIMENSION);
    if (pocket == null) {
      throw helper.assertionException(Component.literal("The pocket dimension does not exist"));
    }

    int centerX = PocketDimensions.centerX(TEST_SLOT);
    int floorY = PocketDimensions.FLOOR_Y;

    PocketDimensions.regenerateCube(pocket, TEST_SLOT);

    // The exit block is the floor's center and the player spawns on top of it.
    assertBlock(helper, pocket, new BlockPos(centerX, floorY, 0),
        LightCraft.POCKET_DIMENSION_EXIT.get().defaultBlockState().getBlock(), "exit block at the floor center");
    assertInsideSpawnIsClear(helper, pocket, centerX, floorY);

    // The outer cube is a one block quartz shell: floor, ceiling and walls.
    for (int dx = -PocketDimensions.CUBE_RADIUS; dx <= PocketDimensions.CUBE_RADIUS; dx++) {
      for (int dz = -PocketDimensions.CUBE_RADIUS; dz <= PocketDimensions.CUBE_RADIUS; dz++) {
        assertBlock(helper, pocket, new BlockPos(centerX + dx, floorY, dz),
            LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "floor layer");
        assertBlock(helper, pocket, new BlockPos(centerX + dx, floorY + PocketDimensions.CUBE_HEIGHT - 1, dz),
            LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "ceiling layer");
      }
    }

    // Damage the cube: blow a hole into a wall, build something inside the
    // interior and drop stray blocks just outside the cube (one next to the
    // wall and one above the ceiling, both within the cleared margin).
    BlockPos wall = new BlockPos(centerX - PocketDimensions.CUBE_RADIUS, floorY + 2, 0);
    BlockPos built = new BlockPos(centerX, floorY + 1, 0);
    BlockPos strayNextToWall = new BlockPos(centerX - PocketDimensions.CUBE_RADIUS - 1, floorY + 2, 0);
    BlockPos strayAboveCeiling = new BlockPos(centerX, floorY + PocketDimensions.CUBE_HEIGHT, 0);
    pocket.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
    pocket.setBlockAndUpdate(built, Blocks.DIRT.defaultBlockState());
    pocket.setBlockAndUpdate(strayNextToWall, Blocks.DIRT.defaultBlockState());
    pocket.setBlockAndUpdate(strayAboveCeiling, Blocks.DIRT.defaultBlockState());

    PocketDimensions.regenerateCube(pocket, TEST_SLOT);

    // The wall is pristine again, the strays are gone - but the interior build
    // survives: the player's cube is their own space.
    assertBlock(helper, pocket, wall,
        LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "restored wall block");
    assertBlock(helper, pocket, built, Blocks.DIRT.defaultBlockState().getBlock(),
        "player build inside the interior preserved");
    assertBlock(helper, pocket, strayNextToWall, Blocks.AIR.defaultBlockState().getBlock(),
        "stray block outside the cube destroyed");
    assertBlock(helper, pocket, strayAboveCeiling, Blocks.AIR.defaultBlockState().getBlock(),
        "stray block above the cube destroyed");

    helper.succeed();
  }

  /**
   * Checks that slots are stable per player (asking twice returns the same
   * slot) and that two different players never share one.
   */
  public static void pocketSlotAllocation(GameTestHelper helper) {
    MinecraftServer server = helper.getLevel().getServer();
    // Deterministic fake players; the saved data of a game test world is
    // throwaway, so allocating slots for them is harmless.
    UUID first = UUID.nameUUIDFromBytes("pocket-test-first".getBytes(StandardCharsets.UTF_8));
    UUID second = UUID.nameUUIDFromBytes("pocket-test-second".getBytes(StandardCharsets.UTF_8));

    PocketDimensionData data = PocketDimensions.data(server);
    int firstSlot = data.allocateSlot(first);
    helper.assertValueEqual(data.allocateSlot(first), firstSlot, "slot stays stable for the same player");
    helper.assertValueEqual(data.slotOf(first), firstSlot, "remembered slot of the player");
    helper.assertValueEqual(data.allocateSlot(second) == firstSlot, false,
        "two different players get different slots");

    helper.succeed();
  }

  /** Asserts the whole 5x5x5 interior - including the block above the exit - is air. */
  private static void assertInsideSpawnIsClear(GameTestHelper helper, ServerLevel pocket, int centerX, int floorY) {
    for (int dx = -PocketDimensions.INTERIOR_RADIUS; dx <= PocketDimensions.INTERIOR_RADIUS; dx++) {
      for (int dy = 1; dy <= PocketDimensions.INTERIOR_HEIGHT; dy++) {
        for (int dz = -PocketDimensions.INTERIOR_RADIUS; dz <= PocketDimensions.INTERIOR_RADIUS; dz++) {
          assertBlock(helper, pocket, new BlockPos(centerX + dx, floorY + dy, dz),
              Blocks.AIR.defaultBlockState().getBlock(), "air inside the cube");
        }
      }
    }
  }

  private static void assertBlock(GameTestHelper helper, ServerLevel pocket, BlockPos pos,
      Block expected, String what) {
    Block actual = pocket.getBlockState(pos).getBlock();
    if (actual != expected) {
      throw helper.assertionException(Component.literal(
          "Expected " + what + " at " + pos.toShortString() + " but found " + actual));
    }
  }
}
