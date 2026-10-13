package dev.ablinger.lightcraft.crop;

import dev.ablinger.lightcraft.LightCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stores the internal data of a luxflower: its suncharge.
 *
 * <p>The suncharge counts the daybreaks the crop witnessed, starting at 0 when it was
 * planted and incremented by 1 at the start of every minecraft day. It is handed to the
 * dropped seed as a data component, so the seed remembers what its crop witnessed, and it
 * determines how many petals a fully grown crop drops when harvested.
 */
public class LuxflowerBlockEntity extends BlockEntity {
  private static final long DAY_LENGTH = 24000L;
  /** Marker for a flower that does not know which day it started growing on yet. */
  private static final long UNKNOWN_DAY = Long.MIN_VALUE;
  private static final String SUNCHARGE_KEY = "suncharge";
  private static final String LAST_DAY_KEY = "last_day";

  private int suncharge = 0;
  private long lastDay = UNKNOWN_DAY;

  public LuxflowerBlockEntity(BlockPos pos, BlockState state) {
    super(LightCraft.LUXFLOWER_BLOCK_ENTITY.get(), pos, state);
  }

  /**
   * @return how many daybreaks this luxflower witnessed, at least 0
   */
  public int getSuncharge() {
    return this.suncharge;
  }

  /**
   * Remembers the suncharge of the seed this luxflower was planted from.
   */
  public void setSuncharge(int suncharge) {
    this.suncharge = suncharge;
    this.setChanged();
  }

  @Override
  protected void loadAdditional(ValueInput input) {
    super.loadAdditional(input);
    this.suncharge = input.getIntOr(SUNCHARGE_KEY, 0);
    this.lastDay = input.getLongOr(LAST_DAY_KEY, UNKNOWN_DAY);
  }

  @Override
  protected void saveAdditional(ValueOutput output) {
    super.saveAdditional(output);
    output.putInt(SUNCHARGE_KEY, this.suncharge);
    output.putLong(LAST_DAY_KEY, this.lastDay);
  }

  /**
   * Ticks the luxflower and increases the suncharge for every started minecraft day.
   * Days spent while the chunk was not loaded are caught up, since the days still passed.
   */
  public static void tick(Level level, BlockPos pos, BlockState state, LuxflowerBlockEntity blockEntity) {
    if (level.isClientSide()) {
      return;
    }

    long currentDay = Math.floorDiv(level.getOverworldClockTime(), DAY_LENGTH);
    if (blockEntity.lastDay == UNKNOWN_DAY) {
      // First tick after planting: remember the day without counting it as a new day.
      blockEntity.lastDay = currentDay;
      blockEntity.setChanged();
    } else if (currentDay != blockEntity.lastDay) {
      if (currentDay > blockEntity.lastDay) {
        long witnessedDaybreaks = currentDay - blockEntity.lastDay;
        blockEntity.suncharge = (int) Math.min(Integer.MAX_VALUE, (long) blockEntity.suncharge + witnessedDaybreaks);
      }
      // If the clock was set back in time, only realign the tracked day.
      blockEntity.lastDay = currentDay;
      blockEntity.setChanged();
    }
  }
}
