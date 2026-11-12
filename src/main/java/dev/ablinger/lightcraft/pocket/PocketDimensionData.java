package dev.ablinger.lightcraft.pocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ablinger.lightcraft.LightCraft;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persistent bookkeeping of the pocket dimension: which player owns which cube
 * slot, and where each player was standing when they teleported in.
 *
 * <p>The data lives in the overworld's saved data storage (see
 * {@link PocketDimensions#data}) because it is cross-dimension bookkeeping, not
 * state of the pocket dimension level itself. 26.x saved data is codec based,
 * so the maps are serialized through unbounded maps keyed by the player UUID
 * as a string.
 *
 * <p>Slots are the identity of a player's private cube: slot {@code n} owns the
 * cube centered at {@code n * SLOT_SPACING} on the x axis, so two players are
 * never closer than {@link PocketDimensions#SLOT_SPACING} blocks apart. A
 * player keeps their slot forever once assigned; nothing here is ever removed
 * on purpose, so a returning player always finds their cube where it was.
 */
public class PocketDimensionData extends SavedData {

  // UUIDs as map keys: unbounded maps need a key codec and UUIDs do not have
  // one, so they travel as their canonical string form.
  private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

  public static final Codec<PocketDimensionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
      Codec.INT.fieldOf("next_slot").forGetter(data -> data.nextSlot),
      Codec.unboundedMap(UUID_CODEC, Codec.INT).fieldOf("slots").forGetter(data -> data.slots),
      Codec.unboundedMap(UUID_CODEC, GlobalPos.CODEC).fieldOf("return_positions").forGetter(data -> data.returnPositions),
      Codec.unboundedMap(UUID_CODEC, Codec.INT).optionalFieldOf("sizes", Map.of()).forGetter(data -> data.sizes)
  ).apply(instance, PocketDimensionData::new));

  public static final SavedDataType<PocketDimensionData> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(LightCraft.MOD_ID, "pocket_dimension"),
      PocketDimensionData::new,
      CODEC);

  /** The slot the next new player gets; monotonically increasing. */
  private int nextSlot;
  /** Player UUID to assigned cube slot. */
  private final Map<UUID, Integer> slots;
  /** Player UUID to where they stood when they last teleported into their cube. */
  private final Map<UUID, GlobalPos> returnPositions;
  /** Player UUID to interior edge length of their cube; absent means the base size. */
  private final Map<UUID, Integer> sizes;

  /** Fresh data for a world that has not seen the pocket dimension yet. */
  public PocketDimensionData() {
    this(0, Map.of(), Map.of(), Map.of());
  }

  public PocketDimensionData(int nextSlot, Map<UUID, Integer> slots, Map<UUID, GlobalPos> returnPositions,
      Map<UUID, Integer> sizes) {
    // Defensive copies: the codec hands us immutable maps, but the game mutates
    // these as players join and teleport.
    this.nextSlot = nextSlot;
    this.slots = new HashMap<>(slots);
    this.returnPositions = new HashMap<>(returnPositions);
    this.sizes = new HashMap<>(sizes);
  }

  /**
   * {@return the player's slot, assigning the next free one on first use}.
   * Assigning marks the data dirty so the new slot reaches disk.
   */
  public int allocateSlot(UUID player) {
    Integer slot = this.slots.get(player);
    if (slot != null) {
      return slot;
    }
    int allocated = this.nextSlot++;
    this.slots.put(player, allocated);
    this.setDirty();
    return allocated;
  }

  /** {@return the player's slot, or null if the player has never visited}. */
  public Integer slotOf(UUID player) {
    return this.slots.get(player);
  }

  /** Remembers where the player stood before teleporting into their cube. */
  public void rememberReturnPosition(UUID player, GlobalPos position) {
    this.returnPositions.put(player, position);
    this.setDirty();
  }

  /** {@return the position the player should be returned to, or null}. */
  public GlobalPos returnPositionOf(UUID player) {
    return this.returnPositions.get(player);
  }

  /** Drops the remembered return position once the player has been returned. */
  public void forgetReturnPosition(UUID player) {
    if (this.returnPositions.remove(player) != null) {
      this.setDirty();
    }
  }

  /**
   * {@return the interior edge length of the player's cube}, falling back to
   * {@link PocketDimensions#BASE_SIZE} for players who never used an upgrade.
   */
  public int sizeOf(UUID player) {
    return this.sizes.getOrDefault(player, PocketDimensions.BASE_SIZE);
  }

  /**
   * Records a new interior edge length for the player's cube. Ignored when the
   * size would not grow, so a stored cube can never shrink.
   */
  public void setSize(UUID player, int size) {
    if (size > this.sizeOf(player)) {
      this.sizes.put(player, size);
      this.setDirty();
    }
  }

  /**
   * Records the size exactly, even downwards. Only the admin command needs
   * this; gameplay growth goes through {@link #setSize}.
   */
  public void forceSize(UUID player, int size) {
    this.sizes.put(player, size);
    this.setDirty();
  }
}
