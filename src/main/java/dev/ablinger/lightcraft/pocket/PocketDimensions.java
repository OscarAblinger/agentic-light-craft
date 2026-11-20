package dev.ablinger.lightcraft.pocket;

import dev.ablinger.lightcraft.LightCraft;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/**
 * The pocket dimension: one private quartz cube per player, floating in a
 * shared void dimension.
 *
 * <p>Geometry (see the constants below): every player owns a slot. Slot
 * {@code n}'s cube is centered at {@code x = n * SLOT_SPACING, z = 0} with its
 * floor at {@code y = FLOOR_Y}. The playable interior starts at
 * {@link #BASE_SIZE} (a 5x5x5 room) and grows to the {@link #UPGRADE_SIZES}
 * through the upgrade items; the walls are one block of indestructible quartz
 * around it. A single bedrock-like exit block sits at the center of the floor
 * and the player always spawns on top of it. Cubes are {@link #SLOT_SPACING}
 * blocks apart, far more than the biggest cube needs, so cubes never collide.
 *
 * <p>Regeneration ({@link #regenerateCube}) repairs the cube: it wipes a
 * margin around the cube (destroying anything a glitched player managed to
 * place outside) and restores the quartz shell and the exit block if they were
 * damaged. The interior is the player's own space and is never touched, so
 * builds inside the cube persist between visits. It runs on every entry and
 * every time a player is found outside their cube. Growing the cube
 * ({@link #expandCube}) additionally frees the old shell so it becomes part of
 * the bigger interior.
 *
 * <p>Return positions and cube sizes are remembered per player in
 * {@link PocketDimensionData}; leaving resolves back to the spot the player
 * teleported in from, falling back to the player's respawn point (or the
 * overworld spawn) if the remembered dimension no longer exists.
 */
public final class PocketDimensions {

  /** The shared void dimension holding every player's cube. See data/lightcraft/dimension*. */
  public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
      Identifier.fromNamespaceAndPath(LightCraft.MOD_ID, "pocket_dimension"));

  /** The horizontal distance between two players' cubes; leaves room to grow the cubes later. */
  public static final int SLOT_SPACING = 10000;
  /** Y of the cube's floor layer; the exit block sits here at the cube center. */
  public static final int FLOOR_Y = 64;
  /** Interior edge length of a fresh cube (a 5x5x5 room). */
  public static final int BASE_SIZE = 5;
  /** Edge lengths the upgrade items grow a cube to, in order. No prerequisites. */
  public static final int[] UPGRADE_SIZES = {7, 9, 11, 15};
  /**
   * On regeneration, everything within this distance outside the walls is
   * destroyed. Regeneration is a repair tool for a player who glitched out, so
   * the margin covers the few blocks they could have drifted or built away
   * from the cube without wiping a large part of the dimension.
   */
  public static final int CLEAR_MARGIN = 8;

  private PocketDimensions() {
  }

  /** {@return the interior radius of a cube with the given edge length}. */
  public static int interiorRadius(int size) {
    return (size - 1) / 2;
  }

  /** {@return the outer radius of a cube: one block of wall around the interior.} */
  public static int cubeRadius(int size) {
    return interiorRadius(size) + 1;
  }

  /** {@return the outer height of a cube: floor + ceiling around the interior.} */
  public static int cubeHeight(int size) {
    return size + 2;
  }

  /** {@return the cross-dimension bookkeeping of the pocket dimension}. */
  public static PocketDimensionData data(MinecraftServer server) {
    return server.overworld().getDataStorage().computeIfAbsent(PocketDimensionData.TYPE);
  }

  /** {@return whether the given level is the pocket dimension}. */
  public static boolean isPocketDimension(Level level) {
    return level.dimension().equals(DIMENSION);
  }

  /** {@return the x center of the cube owned by the given slot}. */
  public static int centerX(int slot) {
    return slot * SLOT_SPACING;
  }

  /**
   * {@return the block position the player spawns at (and is returned to) inside
   * their cube}: standing on the exit block at the floor's center.
   */
  public static Vec3 spawnPoint(int slot) {
    return new Vec3(centerX(slot) + 0.5, FLOOR_Y + 1, 0.5);
  }

  /** {@return whether the block position lies within the interior of the slot's cube at the given size}. */
  public static boolean isInsideCube(int slot, int size, BlockPos pos) {
    int dx = pos.getX() - centerX(slot);
    int dy = pos.getY() - FLOOR_Y;
    int dz = pos.getZ();
    return Math.abs(dx) <= interiorRadius(size)
        && dy >= 1 && dy <= size
        && Math.abs(dz) <= interiorRadius(size);
  }

  /** {@return the player's slot, assigning one on first use}. */
  public static int slotFor(MinecraftServer server, UUID player) {
    return data(server).allocateSlot(player);
  }

  /** {@return the interior edge length of the player's cube}. */
  public static int sizeFor(MinecraftServer server, UUID player) {
    return data(server).sizeOf(player);
  }

  /**
   * Repairs the slot's cube at the given size: loads the chunks the area
   * touches, destroys every block within the surrounding margin (so any block a
   * glitched player put outside the cube is wiped) and restores the quartz
   * shell and the exit block if they were damaged. The interior is left
   * untouched, so whatever the player built inside their cube survives every
   * visit.
   */
  public static void regenerateCube(ServerLevel level, int slot, int size) {
    int centerX = centerX(slot);
    // The full working area: the outer cube inflated by the clear margin. The
    // cube center sits on a chunk border (slot spacing is a multiple of 16), so
    // the area spans a couple of chunks; they must exist before blocks are written.
    BlockPos min = workingAreaMin(centerX, size);
    BlockPos max = workingAreaMax(centerX, size);
    loadArea(level, min, max);

    // Step 1: wipe everything in the working area except the cube itself,
    // destroying stray blocks a glitched-out player left around the cube.
    BlockState air = Blocks.AIR.defaultBlockState();
    for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
      if (isWithinOuterCube(centerX, size, pos)) {
        continue;
      }
      if (!level.getBlockState(pos).isAir()) {
        level.setBlock(pos, air, Block.UPDATE_CLIENTS);
      }
    }

    // Step 2: restore the shell - a one block quartz cube around the (player
    // owned) interior, with the exit block replacing the floor's center. Only
    // mismatched shell blocks are rewritten so the interior stays as the
    // player left it.
    BlockState quartz = LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState();
    for (int dx = -cubeRadius(size); dx <= cubeRadius(size); dx++) {
      for (int dy = 0; dy < cubeHeight(size); dy++) {
        for (int dz = -cubeRadius(size); dz <= cubeRadius(size); dz++) {
          boolean shell = Math.abs(dx) == cubeRadius(size) || Math.abs(dz) == cubeRadius(size)
              || dy == 0 || dy == cubeHeight(size) - 1;
          if (!shell) {
            continue;
          }
          BlockPos pos = new BlockPos(centerX + dx, FLOOR_Y + dy, dz);
          if (!level.getBlockState(pos).is(quartz.getBlock())) {
            level.setBlock(pos, quartz, Block.UPDATE_CLIENTS);
          }
        }
      }
    }
    // The spawn column (exit block and the block above it) is always cleared so
    // the player can never be walled in on entry.
    BlockState exit = LightCraft.POCKET_DIMENSION_EXIT.get().defaultBlockState();
    level.setBlock(new BlockPos(centerX, FLOOR_Y, 0), exit, Block.UPDATE_CLIENTS);
    level.setBlock(new BlockPos(centerX, FLOOR_Y + 1, 0), air, Block.UPDATE_CLIENTS);
  }

  /**
   * Grows the owner's cube to a bigger interior edge length: records the new
   * size, frees the space the old shell occupied (old walls and old ceiling
   * become part of the interior) and regenerates at the new size. The old
   * interior - everything the player built - stays untouched; only structural
   * blocks of the old shell are removed.
   *
   * <p>The pocket dimension level is a parameter (not looked up from the
   * dimension key) so game tests can run the geometry against the test world -
   * the gametest server discards datapack dimensions at world creation.
   *
   * {@return whether the cube grew}; a size that would not grow is silently
   * ignored, so the cube can never shrink.
   */
  public static boolean expandCube(ServerLevel pocket, UUID owner, int size) {
    PocketDimensionData data = data(pocket.getServer());
    int oldSize = data.sizeOf(owner);
    if (size <= oldSize) {
      return false;
    }
    data.setSize(owner, size);

    int slot = data.allocateSlot(owner);
    int centerX = centerX(slot);
    BlockState air = Blocks.AIR.defaultBlockState();
    // The new interior swallows the old shell: every position inside the new
    // interior that was not old interior (old walls, old ceiling, margin
    // leftovers) becomes air. Old interior positions are the player's build
    // and are left alone; the shell step of regenerateCube rebuilds the rest.
    int oldRadius = interiorRadius(oldSize);
    for (int dx = -interiorRadius(size); dx <= interiorRadius(size); dx++) {
      for (int dy = 1; dy <= size; dy++) {
        for (int dz = -interiorRadius(size); dz <= interiorRadius(size); dz++) {
          boolean wasInterior = Math.abs(dx) <= oldRadius && dy <= oldSize && Math.abs(dz) <= oldRadius;
          if (wasInterior) {
            continue;
          }
          BlockPos pos = new BlockPos(centerX + dx, FLOOR_Y + dy, dz);
          if (!pocket.getBlockState(pos).isAir()) {
            pocket.setBlock(pos, air, Block.UPDATE_CLIENTS);
          }
        }
      }
    }
    regenerateCube(pocket, slot, size);
    return true;
  }

  /**
   * Sets the owner's cube to exactly the given size, in either direction - the
   * admin command counterpart of the upgrade items, which can only grow.
   * Growing behaves like {@link #expandCube}; shrinking wipes everything
   * outside the new cube (the old shell and the parts of the interior that no
   * longer fit) and rebuilds the shell at the smaller size. Silently does
   * nothing when the size would not change.
   */
  public static void setCubeSize(ServerLevel pocket, UUID owner, int size) {
    PocketDimensionData data = data(pocket.getServer());
    int oldSize = data.sizeOf(owner);
    if (size == oldSize) {
      return;
    }
    if (size > oldSize) {
      expandCube(pocket, owner, size);
      return;
    }

    data.forceSize(owner, size);
    int slot = data.allocateSlot(owner);
    int centerX = centerX(slot);
    // The old cube's working area is the bigger one; everything in it outside
    // the new outer cube (old shell, trimmed interior, strays) becomes air.
    BlockPos min = workingAreaMin(centerX, oldSize);
    BlockPos max = workingAreaMax(centerX, oldSize);
    loadArea(pocket, min, max);
    BlockState air = Blocks.AIR.defaultBlockState();
    for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
      if (isWithinOuterCube(centerX, size, pos)) {
        continue;
      }
      if (!pocket.getBlockState(pos).isAir()) {
        pocket.setBlock(pos, air, Block.UPDATE_CLIENTS);
      }
    }
    regenerateCube(pocket, slot, size);
  }

  /** {@return the lowest corner of the working area (outer cube + clear margin)}. */
  private static BlockPos workingAreaMin(int centerX, int size) {
    return new BlockPos(centerX - cubeRadius(size) - CLEAR_MARGIN, FLOOR_Y - CLEAR_MARGIN,
        -cubeRadius(size) - CLEAR_MARGIN);
  }

  /** {@return the highest corner of the working area (outer cube + clear margin)}. */
  private static BlockPos workingAreaMax(int centerX, int size) {
    return new BlockPos(centerX + cubeRadius(size) + CLEAR_MARGIN,
        FLOOR_Y + cubeHeight(size) - 1 + CLEAR_MARGIN, cubeRadius(size) + CLEAR_MARGIN);
  }

  /** Force-loads every chunk the given area spans so blocks can be written. */
  private static void loadArea(ServerLevel level, BlockPos min, BlockPos max) {
    for (int chunkX = SectionPos.blockToSectionCoord(min.getX()); chunkX <= SectionPos.blockToSectionCoord(max.getX()); chunkX++) {
      for (int chunkZ = SectionPos.blockToSectionCoord(min.getZ()); chunkZ <= SectionPos.blockToSectionCoord(max.getZ()); chunkZ++) {
        level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
      }
    }
  }

  /** {@return whether the position lies inside the outer cube of the given size (walls included).} */
  private static boolean isWithinOuterCube(int centerX, int size, BlockPos pos) {
    int dy = pos.getY() - FLOOR_Y;
    return Math.abs(pos.getX() - centerX) <= cubeRadius(size)
        && dy >= 0 && dy <= cubeHeight(size) - 1
        && Math.abs(pos.getZ()) <= cubeRadius(size);
  }

  /**
   * Teleports the player into their own pocket cube, remembering where they
   * stood so they can be returned there later. The cube is repaired first, so
   * the walls and exit block are always intact on arrival.
   */
  public static void enter(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    PocketDimensionData data = data(server);
    int slot = data.allocateSlot(player.getUUID());
    data.rememberReturnPosition(player.getUUID(),
        GlobalPos.of(player.level().dimension(), BlockPos.containing(player.position())));

    ServerLevel pocket = server.getLevel(DIMENSION);
    regenerateCube(pocket, slot, data.sizeOf(player.getUUID()));
    teleport(player, pocket, spawnPoint(slot));
  }

  /**
   * Teleports the player out of the pocket dimension: back to where they stood
   * when they entered, or to their respawn point (then the overworld spawn) if
   * that position is no longer available.
   */
  public static void exit(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    PocketDimensionData data = data(server);
    // Resolve first, then forget: forgetting before resolving would always
    // discard the remembered position and fall back to the respawn point.
    GlobalPos target = resolveReturnPosition(server, player);
    data.forgetReturnPosition(player.getUUID());
    ServerLevel level = server.getLevel(target.dimension());
    teleport(player, level, Vec3.atBottomCenterOf(target.pos()));
  }

  /**
   * Returns the player into their cube without touching any remembered return
   * position. Used when a player is found outside their cube: the cube is
   * repaired first, so whatever they did outside is wiped on the way in.
   */
  public static void returnIntoCube(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    int slot = slotFor(server, player.getUUID());
    ServerLevel pocket = server.getLevel(DIMENSION);
    regenerateCube(pocket, slot, sizeFor(server, player.getUUID()));
    teleport(player, pocket, spawnPoint(slot));
  }

  /**
   * The per-tick guard: as soon as a player in the pocket dimension is not
   * inside their interior anymore, they are put back and the cube is repaired
   * around them.
   */
  public static void ensureInsideCube(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    int slot = slotFor(server, player.getUUID());
    int size = sizeFor(server, player.getUUID());
    if (isInsideCube(slot, size, BlockPos.containing(player.position()))) {
      return;
    }
    returnIntoCube(player);
  }

  /**
   * {@return where the player should end up when leaving}: the remembered
   * return position if its dimension still exists, else the player's respawn
   * point, else the overworld spawn.
   */
  private static GlobalPos resolveReturnPosition(MinecraftServer server, ServerPlayer player) {
    GlobalPos remembered = data(server).returnPositionOf(player.getUUID());
    if (remembered != null && server.getLevel(remembered.dimension()) != null) {
      return remembered;
    }

    ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
    if (respawnConfig != null) {
      GlobalPos respawn = respawnConfig.respawnData().globalPos();
      if (server.getLevel(respawn.dimension()) != null) {
        return respawn;
      }
    }

    return server.overworld().getLevelData().getRespawnData().globalPos();
  }

  /**
   * Moves the player, picking a safe landing spot: the destination is
   * force-loaded and, if the exact position ended up inside blocks (the return
   * spot may have been built into a wall since), the player is lifted to the
   * first free position above it.
   */
  private static void teleport(ServerPlayer player, ServerLevel level, Vec3 target) {
    BlockPos landing = BlockPos.containing(target);
    level.getChunk(SectionPos.blockToSectionCoord(landing.getX()),
        SectionPos.blockToSectionCoord(landing.getZ()), ChunkStatus.FULL, true);
    Vec3 safe = findFreePosition(level, target);
    level.playSound(null, BlockPos.containing(safe), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    player.teleportTo(level, safe.x, safe.y, safe.z, Set.of(), player.getYRot(), player.getXRot(), true);
    player.resetFallDistance();
  }

  /** {@return the given position, or the first unobstructed position up to 8 blocks above it}. */
  private static Vec3 findFreePosition(ServerLevel level, Vec3 target) {
    BlockPos start = BlockPos.containing(target);
    for (int dy = 0; dy <= 8; dy++) {
      BlockPos pos = start.above(dy);
      if (isPassable(level, pos) && isPassable(level, pos.above())) {
        return Vec3.atBottomCenterOf(pos);
      }
    }
    return target;
  }

  private static boolean isPassable(ServerLevel level, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    return state.isAir() || state.getCollisionShape(level, pos).isEmpty();
  }
}
