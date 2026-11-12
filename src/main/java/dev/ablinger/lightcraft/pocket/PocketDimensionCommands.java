package dev.ablinger.lightcraft.pocket;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The {@code /pocketdimension} command.
 *
 * <p>Currently a single subcommand, {@code /pocketdimension setsize
 * &lt;players&gt; &lt;size&gt;}, which sets the targets' cube to exactly the
 * given interior size - growing or shrinking - as the game-test-friendly
 * counterpart of the upgrade items (which can only grow and only to the fixed
 * upgrade sizes). Requires permission level 2 (game masters). Sizes must be
 * odd and at least the base size so the cube grows symmetrically around its
 * center column; the upper bound keeps any cube far away from its neighbours.
 *
 * <p>Registered as an instance on the {@code NeoForge.EVENT_BUS} from the
 * mod's main class constructor.
 */
public class PocketDimensionCommands {

  /** Smallest allowed size: the base cube. */
  public static final int MIN_SIZE = PocketDimensions.BASE_SIZE;
  /**
   * Biggest allowed size. Slots are 10000 blocks apart, so this is nowhere
   * near a neighbour, but it keeps fat-fingered numbers from eating them.
   */
  public static final int MAX_SIZE = 99;

  @SubscribeEvent
  public void onRegisterCommands(RegisterCommandsEvent event) {
    event.getDispatcher().register(Commands.literal("pocketdimension")
        .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
        .then(Commands.literal("setsize")
            .then(Commands.argument("players", EntityArgument.players())
                .then(Commands.argument("size", IntegerArgumentType.integer(MIN_SIZE, MAX_SIZE))
                    .executes(context -> {
                      int size = IntegerArgumentType.getInteger(context, "size");
                      // Even sizes have no symmetric cube around the center column.
                      if (size % 2 == 0) {
                        context.getSource().sendFailure(
                            Component.translatable("lightcraft.pocket.command.even_size"));
                        return 0;
                      }
                      int affected = 0;
                      MinecraftServer server = context.getSource().getServer();
                      ServerLevel pocket = server.getLevel(PocketDimensions.DIMENSION);
                      for (ServerPlayer player : EntityArgument.getPlayers(context, "players")) {
                        PocketDimensions.setCubeSize(pocket, player.getUUID(), size);
                        context.getSource().sendSuccess(() -> Component.translatable(
                            "lightcraft.pocket.command.size_set",
                            player.getName().getString(),
                            size + "x" + size + "x" + size), false);
                        affected++;
                      }
                      return affected;
                    })))));
  }
}
