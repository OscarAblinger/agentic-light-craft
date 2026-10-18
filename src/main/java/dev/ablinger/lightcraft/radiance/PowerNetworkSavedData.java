package dev.ablinger.lightcraft.radiance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ablinger.lightcraft.LightCraft;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * The authoritative state of every power network of a level, persisted in the level's
 * {@code data/lightcraft/power_networks.dat} and keyed by {@link NetworkId} (design report
 * 4.3, 11). The pool a network holds must survive chunk unload and reload even when no single
 * member stays loaded, so it lives here rather than in any block entity.
 *
 * <p>Only the player-visible state is persisted: the stored intensity plus the per-node
 * contributions it is derived from (storage capacity, colorizer colour). Everything derived -
 * membership lists, per-tick requests and offers, the live colour - is rebuilt on load and
 * never written out.
 *
 * <p>26.1 persists {@code SavedData} through a {@link Codec}, so this class doubles as its
 * own codec: networks encode as a map of id to their persisted state.
 */
public final class PowerNetworkSavedData extends SavedData {

  /** Codec of a network id; the NBT keeps it as the UUID's string form. */
  private static final Codec<NetworkId> NETWORK_ID_CODEC = Codec.STRING.xmap(
      value -> new NetworkId(UUID.fromString(value)), id -> id.value().toString());

  /** Codec of a node id; nodes are identified by UUID everywhere in their NBT too. */
  private static final Codec<UUID> NODE_ID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

  /** A colorizer's contribution is colour only - its intensity is never persisted. */
  private static final Codec<Radiance> COLOUR_CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.INT.fieldOf("red").forGetter(Radiance::red255),
          Codec.INT.fieldOf("green").forGetter(Radiance::green255),
          Codec.INT.fieldOf("blue").forGetter(Radiance::blue255))
      .apply(instance, (red, green, blue) ->
          new Radiance(red.byteValue(), green.byteValue(), blue.byteValue(), 0L)));

  /** One network's persisted state. */
  private record Persisted(long stored, Map<UUID, Long> storages, Map<UUID, Radiance> colorizers) {

    private static final Codec<Persisted> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("stored").forGetter(Persisted::stored),
            Codec.unboundedMap(NODE_ID_CODEC, Codec.LONG).fieldOf("storages").forGetter(Persisted::storages),
            Codec.unboundedMap(NODE_ID_CODEC, COLOUR_CODEC).fieldOf("colorizers").forGetter(Persisted::colorizers))
        .apply(instance, Persisted::new));
  }

  /** The whole file: every network of the level, keyed by its id. */
  private static final Codec<PowerNetworkSavedData> CODEC = Codec.unboundedMap(NETWORK_ID_CODEC, Persisted.CODEC)
      .xmap(PowerNetworkSavedData::restore, PowerNetworkSavedData::persist);

  public static final SavedDataType<PowerNetworkSavedData> TYPE = new SavedDataType<>(
      Identifier.fromNamespaceAndPath(LightCraft.MOD_ID, "power_networks"),
      PowerNetworkSavedData::new,
      CODEC);

  /** The live networks of this level, keyed by id; mutated only through the manager. */
  private final Map<NetworkId, PowerNetwork> networks = new LinkedHashMap<>();

  /** The tick driver owning these networks; in-memory only and rebuilt per level load. */
  private PowerNetworkManager manager;

  public static PowerNetworkSavedData get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(TYPE);
  }

  /** The networks of this level, keyed by id; read-only, as only the manager may change them. */
  public Map<NetworkId, PowerNetwork> networks() {
    return Collections.unmodifiableMap(networks);
  }

  /** The network of the given id, created - empty, ready to be filled - when it is missing. */
  PowerNetwork getOrCreate(NetworkId id) {
    PowerNetwork network = networks.get(id);
    if (network == null) {
      network = new PowerNetwork(id);
      networks.put(id, network);
      setDirty();
    }
    return network;
  }

  void put(PowerNetwork network) {
    networks.put(network.id(), network);
    setDirty();
  }

  void remove(NetworkId id) {
    if (networks.remove(id) != null) {
      setDirty();
    }
  }

  PowerNetworkManager manager() {
    if (manager == null) {
      manager = new PowerNetworkManager(this);
    }
    return manager;
  }

  private static PowerNetworkSavedData restore(Map<NetworkId, Persisted> persisted) {
    PowerNetworkSavedData data = new PowerNetworkSavedData();
    persisted.forEach((id, state) -> {
      PowerNetwork network = new PowerNetwork(id);
      network.restore(state.stored(), state.storages(), state.colorizers());
      data.networks.put(id, network);
    });
    return data;
  }

  private Map<NetworkId, Persisted> persist() {
    Map<NetworkId, Persisted> persisted = new LinkedHashMap<>();
    for (PowerNetwork network : networks.values()) {
      persisted.put(network.id(), new Persisted(network.intensity(), network.storages(), network.colorizers()));
    }
    return persisted;
  }
}
