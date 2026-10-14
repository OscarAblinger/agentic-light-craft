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

/**
 * The luxflower crop.
 *
 * <p>It grows half as fast as wheat by accepting only every second random tick growth
 * attempt. Its block entity counts the daybreaks it witnessed, its suncharge. Harvesting
 * always drops the planted seed remembering that suncharge, and a fully grown crop
 * additionally drops one petal per witnessed daybreak.
 */
public class LuxflowerBlock extends CropBlock implements EntityBlock {

  public LuxflowerBlock(BlockBehaviour.Properties properties) {
    super(properties);
  }

  @Override
  protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    // Halve the growth rate compared to wheat: skip every second growth attempt.
    if (random.nextInt(2) != 0) {
      return;
    }
    super.randomTick(state, level, pos, random);
  }

  /**
   * Computes the harvest of the crop.
   *
   * <p>The loot table provides the single planted seed. The block breaking code captures
   * the still live block entity before removing the crop and passes it into the loot
   * context under {@link LootContextParams#BLOCK_ENTITY}, so the suncharge recorded while
   * growing can be read from {@link LuxflowerBlockEntity#getSuncharge()} here: the seed is
   * stamped with it, and a fully grown crop drops one petal per witnessed daybreak.
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

    // The petals are the day based harvest of a mature crop. They drop compressed into
    // bundles so a large suncharge does not litter thousands of loose petals.
    if (suncharge > 0 && this.isMaxAge(state)) {
      PetalBundles.decompose(suncharge, drops::add);
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
