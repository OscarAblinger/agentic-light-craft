package dev.ablinger.lightcraft.crop;

import dev.ablinger.lightcraft.LightCraft;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * The luxflower crop.
 *
 * <p>It grows like wheat, but only while the sun is up and under wheat's light
 * requirement, and its growth is tuned so a planted seed reaches full size after one
 * minecraft day on average. Its block entity counts the daybreaks it witnessed, its
 * suncharge. Harvesting always drops the planted seed remembering that suncharge, and
 * a fully grown crop drops one petal plus one additional petal per witnessed daybreak.
 *
 * <p>From the third growth stage on the luxflower emits a faint light, twice as bright
 * when fully grown, so a planted field glows at night while the crop rests.
 */
public class LuxflowerBlock extends CropBlock implements EntityBlock {

  /** Length of a full minecraft day and of its day and night halves, in game ticks. */
  private static final long DAY_LENGTH = 24000L;
  private static final long NIGHT_LENGTH = DAY_LENGTH / 2;

  /** Wheat's growth requirement: at least this much raw light at the crop's position. */
  private static final int MIN_GROWTH_LIGHT = 9;

  /**
   * Growth is rolled on every random growth attempt: GROWTH_SUCCESSES out of
   * GROWTH_ROLLS attempts advance the crop by one stage. Random ticks hit every
   * block with a 3 / 4096 chance per game tick (three rolled positions per 4096
   * block chunk section, see {@code ServerLevel#tickChunk}), so the 12000 ticks
   * of daylight hold 12000 * 3 / 4096 = 8.79 random ticks on average. That makes
   * a luxflower advance 8.79 * 4 / 5 = 7.03 stages per day - a full growth of its
   * seven stages - on average, while individual crops still vary around that.
   */
  private static final int GROWTH_ROLLS = 5;
  private static final int GROWTH_SUCCESSES = 4;

  /** Age of the third growth stage, from which the luxflower starts glowing. */
  private static final int FIRST_GLOWING_AGE = 2;

  public LuxflowerBlock(BlockBehaviour.Properties properties) {
    // A faint glow from the third growth stage on, twice as bright when fully grown.
    super(properties.lightLevel(LuxflowerBlock::lightAt));
  }

  /**
   * Grows the crop, but only while the sun is up: luxflowers feed on daylight and rest
   * through the night, no matter how bright their surroundings are. Otherwise the
   * conditions match wheat's - a loaded area, a raw light level of at least 9 - and
   * every growth attempt advances a stage with the tuned probability of
   * {@link #GROWTH_SUCCESSES} / {@link #GROWTH_ROLLS}.
   */
  @Override
  protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    // The overworld clock decides day and night - the same clock the daybreaks the
    // block entity counts are taken from, so growth and suncharge stay in tune.
    if (!isDaytime(level) || !level.isAreaLoaded(pos, 1) || level.getRawBrightness(pos, 0) < MIN_GROWTH_LIGHT) {
      return;
    }

    int age = this.getAge(state);
    if (age >= this.getMaxAge()) {
      return;
    }

    // Most, but not all, growth attempts succeed, keeping individual crops varying
    // around the one day average described at GROWTH_ROLLS.
    if (random.nextInt(GROWTH_ROLLS) >= GROWTH_SUCCESSES) {
      return;
    }

    if (CommonHooks.canCropGrow(level, pos, state, true)) {
      level.setBlock(pos, this.getStateForAge(age + 1), 2);
      CommonHooks.fireCropGrowPost(level, pos, state);
    }
  }

  /** Whether the sun is up: the first half of the current minecraft day. */
  private static boolean isDaytime(Level level) {
    return Math.floorMod(level.getOverworldClockTime(), DAY_LENGTH) < NIGHT_LENGTH;
  }

  /** Glow of the luxflower at the given growth stage. */
  private static int lightAt(BlockState state) {
    int age = state.getValue(CropBlock.AGE);
    if (age >= CropBlock.MAX_AGE) {
      return 2;
    }
    return age >= FIRST_GLOWING_AGE ? 1 : 0;
  }

  /**
   * Computes the harvest of the crop.
   *
   * <p>The loot table provides the single planted seed. The block breaking code captures
   * the still live block entity before removing the crop and passes it into the loot
   * context under {@link LootContextParams#BLOCK_ENTITY}, so the suncharge recorded while
   * growing can be read from {@link LuxflowerBlockEntity#getSuncharge()} here: the seed is
   * stamped with it, and a fully grown crop drops one petal plus one additional petal per
   * witnessed daybreak.
   */
  @Override
  protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
    List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));

    BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
    int suncharge = blockEntity instanceof LuxflowerBlockEntity luxflower ? luxflower.getSuncharge() : 0;

    // The single dropped seed remembers the suncharge of its crop.
    for (ItemStack drop : drops) {
      if (drop.is(LightCraft.LUXFLOWER_SEEDS.get())) {
        drop.set(LightCraft.SUNCHARGE.get(), suncharge);
      }
    }

    // The petals are the day based harvest of a mature crop: one petal for the flower
    // itself plus one petal per witnessed daybreak, so even a crop that never saw a
    // daybreak yields something. They drop compressed into bundles so a large
    // suncharge does not litter thousands of loose petals.
    if (this.isMaxAge(state)) {
      PetalBundles.decompose(suncharge + 1, drops::add);
    }
    return drops;
  }

  /**
   * Hands the suncharge of the planted seed over to the new crop, so a remembering seed
   * continues to count up from what it witnessed when it is replanted.
   */
  @Override
  public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity by, ItemStack itemStack) {
    super.setPlacedBy(level, pos, state, by, itemStack);
    if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LuxflowerBlockEntity luxflower) {
      luxflower.setSuncharge(itemStack.getOrDefault(LightCraft.SUNCHARGE.get(), 0));
    }
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new LuxflowerBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
    if (level.isClientSide()) {
      return null;
    }
    return createTickerHelper(type, LightCraft.LUXFLOWER_BLOCK_ENTITY.get(), LuxflowerBlockEntity::tick);
  }

  @SuppressWarnings("unchecked")
  private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
      BlockEntityType<A> actualType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
    return actualType == expectedType ? (BlockEntityTicker<A>) ticker : null;
  }
}
