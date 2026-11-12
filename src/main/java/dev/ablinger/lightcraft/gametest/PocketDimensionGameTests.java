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
 * dimensions: the tests build a cube far out in the test level itself - the
 * gametest server bakes its dimensions from an empty datapack dimension
 * registry, so the real pocket dimension does not exist there - and verify
 * that walls, interior, exit block, the cleanup of stray outside blocks and
 * the size upgrades all behave.
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
    // The gametest server drops datapack dimensions, so the geometry runs in
    // the test level; nothing here depends on being in the pocket dimension.
    ServerLevel pocket = helper.getLevel();

    int centerX = PocketDimensions.centerX(TEST_SLOT);
    int floorY = PocketDimensions.FLOOR_Y;
    int size = PocketDimensions.BASE_SIZE;
    int cubeRadius = PocketDimensions.cubeRadius(size);
    int cubeHeight = PocketDimensions.cubeHeight(size);

    PocketDimensions.regenerateCube(pocket, TEST_SLOT, size);

    // The exit block is the floor's center and the player spawns on top of it.
    assertBlock(helper, pocket, new BlockPos(centerX, floorY, 0),
        LightCraft.POCKET_DIMENSION_EXIT.get().defaultBlockState().getBlock(), "exit block at the floor center");
    assertInsideIsClear(helper, pocket, centerX, floorY, size);

    // The outer cube is a one block quartz shell: floor, ceiling and walls.
    // The floor's center is the exit block, not quartz.
    for (int dx = -cubeRadius; dx <= cubeRadius; dx++) {
      for (int dz = -cubeRadius; dz <= cubeRadius; dz++) {
        if (dx == 0 && dz == 0) {
          continue;
        }
        assertBlock(helper, pocket, new BlockPos(centerX + dx, floorY, dz),
            LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "floor layer");
        assertBlock(helper, pocket, new BlockPos(centerX + dx, floorY + cubeHeight - 1, dz),
            LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "ceiling layer");
      }
    }

    // Damage the cube: blow a hole into a wall, build something inside the
    // interior and drop stray blocks just outside the cube (one next to the
    // wall and one above the ceiling, both within the cleared margin).
    BlockPos wall = new BlockPos(centerX - cubeRadius, floorY + 2, 0);
    // A corner of the interior, deliberately not the spawn column above the
    // exit: that two blocks are always kept clear so nobody can wall in entry.
    BlockPos built = new BlockPos(centerX + 1, floorY + 1, 1);
    BlockPos strayNextToWall = new BlockPos(centerX - cubeRadius - 1, floorY + 2, 0);
    BlockPos strayAboveCeiling = new BlockPos(centerX, floorY + cubeHeight, 0);
    pocket.setBlockAndUpdate(wall, Blocks.AIR.defaultBlockState());
    pocket.setBlockAndUpdate(built, Blocks.DIRT.defaultBlockState());
    pocket.setBlockAndUpdate(strayNextToWall, Blocks.DIRT.defaultBlockState());
    pocket.setBlockAndUpdate(strayAboveCeiling, Blocks.DIRT.defaultBlockState());

    PocketDimensions.regenerateCube(pocket, TEST_SLOT, size);

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
   * Grows a cube straight from the base size to 11x11x11 - skipping the 7 and
   * 9 upgrades on purpose, since upgrades have no prerequisites - verifies the
   * old shell became interior, the player's build survived, the new shell was
   * built and same/smaller upgrades are refused, then shrinks it back to the
   * base size the way the /pocketdimension setsize command does.
   */
  public static void pocketCubeExpansion(GameTestHelper helper) {
    // The gametest server drops datapack dimensions, so the geometry runs in
    // the test level; nothing here depends on being in the pocket dimension.
    ServerLevel pocket = helper.getLevel();
    MinecraftServer server = pocket.getServer();

    // Deterministic fake owner; the saved data of a game test world is
    // throwaway, so owning a cube for it is harmless.
    UUID owner = UUID.nameUUIDFromBytes("pocket-test-expansion".getBytes(StandardCharsets.UTF_8));
    PocketDimensionData data = PocketDimensions.data(server);
    data.allocateSlot(owner);
    int slot = data.slotOf(owner);
    int centerX = PocketDimensions.centerX(slot);
    int floorY = PocketDimensions.FLOOR_Y;

    int base = PocketDimensions.BASE_SIZE;
    PocketDimensions.regenerateCube(pocket, slot, base);
    BlockPos oldWall = new BlockPos(centerX - PocketDimensions.cubeRadius(base), floorY + 2, 0);
    BlockPos oldCeiling = new BlockPos(centerX, floorY + PocketDimensions.cubeHeight(base) - 1, 0);
    BlockPos built = new BlockPos(centerX + 1, floorY + 1, 1);
    assertBlock(helper, pocket, oldWall,
        LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "old shell wall before expansion");
    pocket.setBlockAndUpdate(built, Blocks.DIRT.defaultBlockState());

    int grown = 11;
    helper.assertValueEqual(PocketDimensions.expandCube(pocket, owner, grown), true, "cube grows to the new size");
    helper.assertValueEqual(data.sizeOf(owner), grown, "new size recorded for the owner");

    // The old walls and old ceiling are interior now and were cleared...
    assertBlock(helper, pocket, oldWall, Blocks.AIR.defaultBlockState().getBlock(),
        "old shell wall freed for the interior");
    assertBlock(helper, pocket, oldCeiling, Blocks.AIR.defaultBlockState().getBlock(),
        "old shell ceiling freed for the interior");
    // ...the new shell surrounds the bigger room, and builds are untouched.
    assertBlock(helper, pocket, new BlockPos(centerX - PocketDimensions.cubeRadius(grown), floorY + 2, 0),
        LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "new shell wall after expansion");
    assertBlock(helper, pocket, built, Blocks.DIRT.defaultBlockState().getBlock(),
        "player build survives the expansion");
    assertBlock(helper, pocket, new BlockPos(centerX, floorY, 0),
        LightCraft.POCKET_DIMENSION_EXIT.get().defaultBlockState().getBlock(), "exit block after expansion");

    // Same or smaller sizes do nothing.
    helper.assertValueEqual(PocketDimensions.expandCube(pocket, owner, grown), false, "same-size upgrade does nothing");
    helper.assertValueEqual(PocketDimensions.expandCube(pocket, owner, 7), false, "smaller upgrade does nothing");
    helper.assertValueEqual(data.sizeOf(owner), grown, "size unchanged by refused upgrades");

    // The admin command can also shrink (setCubeSize): the shell moves in,
    // everything that no longer fits is wiped, builds inside the new room
    // survive and the size is recorded exactly - not just grown. A block in
    // the big room that falls outside the shrunk cube is wiped on the way.
    BlockPos trimmed = new BlockPos(centerX - 5, floorY + 2, 0);
    pocket.setBlockAndUpdate(trimmed, Blocks.DIRT.defaultBlockState());
    PocketDimensions.setCubeSize(pocket, owner, base);
    helper.assertValueEqual(data.sizeOf(owner), base, "command shrinks back to the base size");
    assertBlock(helper, pocket, trimmed, Blocks.AIR.defaultBlockState().getBlock(),
        "build outside the shrunk cube cleared");
    assertBlock(helper, pocket, new BlockPos(centerX - PocketDimensions.cubeRadius(base), floorY + 2, 0),
        LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "shell wall at the smaller size");
    assertBlock(helper, pocket, oldCeiling,
        LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState().getBlock(), "ceiling rebuilt at the smaller size");
    assertBlock(helper, pocket, built, Blocks.DIRT.defaultBlockState().getBlock(),
        "player build inside the shrunk interior preserved");

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

  /** Asserts the whole interior of the given size - including the block above the exit - is air. */
  private static void assertInsideIsClear(GameTestHelper helper, ServerLevel pocket, int centerX, int floorY, int size) {
    int radius = PocketDimensions.interiorRadius(size);
    for (int dx = -radius; dx <= radius; dx++) {
      for (int dy = 1; dy <= size; dy++) {
        for (int dz = -radius; dz <= radius; dz++) {
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
