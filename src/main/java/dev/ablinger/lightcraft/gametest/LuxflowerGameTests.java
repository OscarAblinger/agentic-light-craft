package dev.ablinger.lightcraft.gametest;

import dev.ablinger.lightcraft.LightCraft;
import dev.ablinger.lightcraft.crop.LuxflowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Game tests for the suncharge of the luxflower crop.
 *
 * <p>Harvesting always drops the planted seed remembering the witnessed daybreaks as its
 * suncharge, while a fully grown crop additionally drops one petal per witnessed daybreak.
 * A replanted seed continues counting up from the suncharge it remembers.
 *
 * <p>All scenarios share a single test function because the minecraft day is a single
 * global world clock that concurrent tests would fight over.
 */
public final class LuxflowerGameTests {

  /** Length of a full minecraft day in ticks. */
  private static final long DAY_LENGTH = 24000L;
  /** Horizontal distance between the scenarios, keeps their dropped items apart. */
  private static final int SPACING = 5;

  private LuxflowerGameTests() {
  }

  /**
   * Harvests one immature and one mature luxflower without a passed day, then grows a
   * second mature luxflower over two full minecraft days, and checks the drops and the
   * suncharge of all four crops. The fourth crop grows from the seed the third crop
   * dropped, remembering its suncharge of 2, which keeps counting up from there.
   */
  public static void luxflowerSeedDrops(GameTestHelper helper) {
    startAtKnownDay(helper);
    placeCrop(helper, 0, 0);
    placeCrop(helper, 1, 0);
    placeCrop(helper, 2, 0);
    placeSoil(helper, 3);

    // The block entities remember the day their crop started growing on, no daybreak
    // happened yet, so nothing new is witnessed and no petals are earned.
    helper.runAfterDelay(2, () -> {
      assertSuncharge(helper, 0, 0);
      assertSuncharge(helper, 1, 0);
      assertSuncharge(helper, 2, 0);
    });

    // Harvest the immature crop while the other two grow to full size.
    helper.runAfterDelay(4, () -> {
      harvest(helper, 0);
      growToFullSize(helper, 1);
      growToFullSize(helper, 2);
    });

    // An immature crop drops exactly its own seed, which remembers a suncharge of 0.
    helper.runAfterDelay(7, () -> {
      assertHarvest(helper, 0, 0, 0);
      harvest(helper, 1);
    });

    // A mature crop that did not cross a day boundary adds no petals either.
    helper.runAfterDelay(10, () -> {
      assertHarvest(helper, 1, 0, 0);
      advanceDays(helper, 2);
    });

    // The mature crop catches up the two passed daybreaks in its suncharge, then is
    // harvested, dropping its remembering seed and two petals.
    helper.runAfterDelay(14, () -> {
      assertSuncharge(helper, 2, 2);
      harvest(helper, 2);
    });

    // A mature crop drops the remembering seed plus one petal per witnessed daybreak.
    // Replanting that very dropped seed continues the counting from its suncharge.
    helper.runAfterDelay(17, () -> {
      assertHarvest(helper, 2, 2, 2);
      plantSeed(helper, 3, droppedSeed(helper, 2));
    });

    // The replanted crop witnessed the day it was planted on without a new daybreak,
    // then grows to full size while one more day passes.
    helper.runAfterDelay(21, () -> {
      assertSuncharge(helper, 3, 2);
      advanceDays(helper, 1);
      growToFullSize(helper, 3);
    });

    // The replanted crop witnessed one more daybreak on top of the remembered suncharge.
    helper.runAfterDelay(24, () -> {
      assertSuncharge(helper, 3, 3);
      harvest(helper, 3);
    });

    // Fully grown with three witnessed daybreaks: the seed remembers 3 and 3 petals drop.
    helper.runAfterDelay(27, () -> {
      assertHarvest(helper, 3, 3, 3);
      helper.succeed();
    });
  }

  /**
   * Resets the world clock to a fixed point in the day so no unexpected day boundary
   * is crossed while the short test runs.
   */
  private static void startAtKnownDay(GameTestHelper helper) {
    ServerLevel level = helper.getLevel();
    level.clockManager().setTotalTicks(overworldClock(helper), DAY_LENGTH);
  }

  private static void placeSoil(GameTestHelper helper, int scenario) {
    helper.setBlock(support(scenario), Blocks.STONE);
    helper.setBlock(farmland(scenario), Blocks.FARMLAND);
  }

  private static void placeCrop(GameTestHelper helper, int scenario, int age) {
    placeSoil(helper, scenario);
    helper.setBlock(crop(scenario), LightCraft.LUXFLOWER.get().getStateForAge(age));
  }

  private static void growToFullSize(GameTestHelper helper, int scenario) {
    // Setting the age on the same block keeps the block entity with its suncharge.
    helper.setBlock(crop(scenario), LightCraft.LUXFLOWER.get().getStateForAge(7));
  }

  /**
   * Plants the given seed the same way a player would plant it, running the block item
   * through the placement logic of the game.
   */
  private static void plantSeed(GameTestHelper helper, int scenario, ItemStack seeds) {
    ServerLevel level = helper.getLevel();
    BlockPos cropPos = helper.absolutePos(crop(scenario));
    BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(cropPos), Direction.UP, cropPos, true);
    BlockPlaceContext context = new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, seeds, hitResult);
    InteractionResult result = ((BlockItem) LightCraft.LUXFLOWER_SEEDS.get()).place(context);
    if (!result.consumesAction()) {
      throw helper.assertionException(Component.literal("The seed could not be planted on scenario " + scenario));
    }
  }

  private static void advanceDays(GameTestHelper helper, int days) {
    helper.getLevel().clockManager().addTicks(overworldClock(helper), (int) (days * DAY_LENGTH));
  }

  private static void harvest(GameTestHelper helper, int scenario) {
    // Destroy with drops enabled, the built in helper.destroyBlock would drop nothing.
    helper.getLevel().destroyBlock(helper.absolutePos(crop(scenario)), true, null);
  }

  private static void assertSuncharge(GameTestHelper helper, int scenario, int expected) {
    int suncharge = helper.getBlockEntity(crop(scenario), LuxflowerBlockEntity.class).getSuncharge();
    helper.assertValueEqual(suncharge, expected, "recorded suncharge");
  }

  /**
   * Asserts the harvest of a crop: always the remembering seed, and one petal per
   * witnessed daybreak if the crop was fully grown.
   */
  private static void assertHarvest(GameTestHelper helper, int scenario, int expectedSuncharge, int expectedPetals) {
    helper.assertItemEntityCountIs(LightCraft.LUXFLOWER_SEEDS.get().asItem(), crop(scenario), 2.0, 1);
    helper.assertValueEqual(droppedSeed(helper, scenario).getOrDefault(LightCraft.SUNCHARGE.get(), 0),
        expectedSuncharge, "suncharge of dropped seed");
    helper.assertItemEntityCountIs(LightCraft.LUXFLOWER_PETAL.get().asItem(), crop(scenario), 2.0, expectedPetals);
  }

  private static ItemStack droppedSeed(GameTestHelper helper, int scenario) {
    AABB area = new AABB(helper.absolutePos(crop(scenario))).inflate(2.0);
    for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area, Entity::isAlive)) {
      if (entity.getItem().is(LightCraft.LUXFLOWER_SEEDS.get())) {
        return entity.getItem();
      }
    }
    throw helper.assertionException(Component.literal("No dropped luxflower seed was found for scenario " + scenario));
  }

  private static Holder<WorldClock> overworldClock(GameTestHelper helper) {
    return helper.getLevel().registryAccess().getOrThrow(WorldClocks.OVERWORLD);
  }

  private static BlockPos support(int scenario) {
    return new BlockPos(scenario * SPACING, -1, 0);
  }

  private static BlockPos farmland(int scenario) {
    return new BlockPos(scenario * SPACING, 0, 0);
  }

  private static BlockPos crop(int scenario) {
    return new BlockPos(scenario * SPACING, 1, 0);
  }
}
