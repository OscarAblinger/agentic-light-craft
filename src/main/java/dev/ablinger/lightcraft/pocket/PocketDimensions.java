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
 * The pocket dimension: one private 5x5x5 quartz cube per player, floating in
 * a shared void dimension.
 *
 * <p>Geometry (see the constants below): every player owns a slot. Slot
 * {@code n}'s cube is centered at {@code x = n * SLOT_SPACING, z = 0} with its
 * floor at {@code y = FLOOR_Y}. The playable interior is 5x5x5 blocks, the walls
 * are one block of indestructible quartz, so the outer cube is 7x7x7. A single
 * bedrock-like exit block sits at the center of the floor and the player always
 * spawns on top of it. Cubes are {@link #SLOT_SPACING} blocks apart so future
 * cube-size upgrades have room to grow without colliding.
 *
 * <p>Regeneration ({@link #regenerateCube}) is the authority on what a cube
 * looks like: it wipes a margin around the cube (destroying anything a glitched
 * player managed to place outside) and rebuilds the walls, the air and the exit
 * block from scratch. It runs on every entry and every time a player is found
 * outside their cube, so damage can never persist.
 *
 * <p>Return positions are remembered per player in {@link PocketDimensionData}
 * when they teleport in; leaving resolves back to exactly that spot, falling
 * back to the player's respawn point (or the overworld spawn) if the remembered
 * dimension no longer exists.
 */
public final class PocketDimensions {

  /** The shared void dimension holding every player's cube. See data/lightcraft/dimension*. */
  public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION,
      Identifier.fromNamespaceAndPath(LightCraft.MOD_ID, "pocket_dimension"));

  /** Horizontal distance between two players' cubes; leaves room to grow the cubes later. */
  public static final int SLOT_SPACING = 10000;
  /** Y of the cube's floor layer; the exit block sits here at the cube center. */
  public static final int FLOOR_Y = 64;
  /** The playable interior is 5x5x5: radius 2 around the center column, 5 tall. */
  public static final int INTERIOR_RADIUS = 2;
  public static final int INTERIOR_HEIGHT = 5;
  /** Walls are one block thick, so the outer cube is 7x7x7. */
  public static final int CUBE_RADIUS = INTERIOR_RADIUS + 1;
  public static final int CUBE_HEIGHT = INTERIOR_HEIGHT + 2;
  /**
   * On regeneration, everything within this distance outside the walls is
   * destroyed. Regeneration is a repair tool for a player who glitched out, so
   * the margin covers the few blocks they could have drifted or built away
   * from the cube without wiping a large part of the dimension.
   */
  public static final int CLEAR_MARGIN = 8;

  private PocketDimensions() {
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

  /** {@return whether the block position lies within the 5x5x5 interior of the slot's cube}. */
  public static boolean isInsideCube(int slot, BlockPos pos) {
    int dx = pos.getX() - centerX(slot);
    int dy = pos.getY() - FLOOR_Y;
    int dz = pos.getZ();
    return Math.abs(dx) <= INTERIOR_RADIUS
        && dy >= 1 && dy <= INTERIOR_HEIGHT
        && Math.abs(dz) <= INTERIOR_RADIUS;
  }

  /** {@return the player's slot, assigning one on first use}. */
  public static int slotFor(MinecraftServer server, UUID player) {
    return data(server).allocateSlot(player);
  }

  /**
   * Rebuilds the slot's cube from scratch: loads the chunks the area touches,
   * destroys every block within the cube plus the surrounding margin (so any
   * block a glitched player put outside the cube is wiped) and places the
   * quartz walls, the air interior and the exit block again.
   */
  public static void regenerateCube(ServerLevel level, int slot) {
    int centerX = centerX(slot);
    // The full working area: the outer cube inflated by the clear margin. The
    // cube center sits on a chunk border (slot spacing is a multiple of 16), so
    // the area spans 2x2 chunks; they must exist before blocks are written.
    BlockPos min = new BlockPos(centerX - CUBE_RADIUS - CLEAR_MARGIN, FLOOR_Y - CLEAR_MARGIN, -CUBE_RADIUS - CLEAR_MARGIN);
    BlockPos max = new BlockPos(centerX + CUBE_RADIUS + CLEAR_MARGIN, FLOOR_Y + CUBE_HEIGHT - 1 + CLEAR_MARGIN, CUBE_RADIUS + CLEAR_MARGIN);
    for (int chunkX = SectionPos.blockToSectionCoord(min.getX()); chunkX <= SectionPos.blockToSectionCoord(max.getX()); chunkX++) {
      for (int chunkZ = SectionPos.blockToSectionCoord(min.getZ()); chunkZ <= SectionPos.blockToSectionCoord(max.getZ()); chunkZ++) {
        level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
      }
    }

    // Step 1: wipe everything in the working area, destroying stray blocks.
    BlockState air = Blocks.AIR.defaultBlockState();
    for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
      if (!level.getBlockState(pos).isAir()) {
        level.setBlock(pos, air, Block.UPDATE_CLIENTS);
      }
    }

    // Step 2: rebuild the cube - a one block quartz shell around a 5x5x5 air
    // interior, with the exit block replacing the floor's center.
    BlockState quartz = LightCraft.POCKET_DIMENSION_QUARTZ.get().defaultBlockState();
    BlockState exit = LightCraft.POCKET_DIMENSION_EXIT.get().defaultBlockState();
    for (int dx = -CUBE_RADIUS; dx <= CUBE_RADIUS; dx++) {
      for (int dy = 0; dy < CUBE_HEIGHT; dy++) {
        for (int dz = -CUBE_RADIUS; dz <= CUBE_RADIUS; dz++) {
          boolean shell = Math.abs(dx) == CUBE_RADIUS || Math.abs(dz) == CUBE_RADIUS
              || dy == 0 || dy == CUBE_HEIGHT - 1;
          BlockPos pos = new BlockPos(centerX + dx, FLOOR_Y + dy, dz);
          BlockState wanted = shell ? quartz : air;
          if (!level.getBlockState(pos).is(wanted.getBlock())) {
            level.setBlock(pos, wanted, Block.UPDATE_CLIENTS);
          }
        }
      }
    }
    level.setBlock(new BlockPos(centerX, FLOOR_Y, 0), exit, Block.UPDATE_CLIENTS);
  }

  /**
   * Teleports the player into their own pocket cube, remembering where they
   * stood so they can be returned there later. The cube is regenerated first,
   * so every visit starts from a pristine interior.
   */
  public static void enter(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    PocketDimensionData data = data(server);
    int slot = data.allocateSlot(player.getUUID());
    data.rememberReturnPosition(player.getUUID(),
        GlobalPos.of(player.level().dimension(), BlockPos.containing(player.position())));

    ServerLevel pocket = server.getLevel(DIMENSION);
    regenerateCube(pocket, slot);
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
    data.forgetReturnPosition(player.getUUID());

    GlobalPos target = resolveReturnPosition(server, player);
    ServerLevel level = server.getLevel(target.dimension());
    teleport(player, level, Vec3.atBottomCenterOf(target.pos()));
  }

  /**
   * Returns the player into their cube without touching any remembered return
   * position. Used when a player is found outside their cube: the cube is
   * regenerated first, so whatever they did out there is wiped on the way in.
   */
  public static void returnIntoCube(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    int slot = slotFor(server, player.getUUID());
    ServerLevel pocket = server.getLevel(DIMENSION);
    regenerateCube(pocket, slot);
    teleport(player, pocket, spawnPoint(slot));
  }

  /**
   * The per-tick guard: as soon as a player in the pocket dimension is not
   * inside their 5x5x5 interior anymore, they are put back and the cube is
   * regenerated around them.
   */
  public static void ensureInsideCube(ServerPlayer player) {
    MinecraftServer server = player.level().getServer();
    int slot = slotFor(server, player.getUUID());
    if (isInsideCube(slot, BlockPos.containing(player.position()))) {
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
