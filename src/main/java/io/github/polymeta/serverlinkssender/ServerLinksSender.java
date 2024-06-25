package io.github.polymeta.serverlinkssender;

import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateContext;
import io.github.polymeta.serverlinkssender.config.BaseConfig;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class ServerLinksSender implements ModInitializer
{
    public static final Logger LOGGER = LogManager.getLogger("Server Link Sender");
    public static final NodeParser PARSER = NodeParser.builder()
            .simplifiedTextFormat()
            .quickText()
            .globalPlaceholders()
            .staticPreParsing()
            .build();
    public static BaseConfig CONFIG;

    private static final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onInitialize()
    {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
        {
            dispatcher.register(
                    literal("reloadserverlinks")
                            .requires(Permissions.require("serverlinks.main", 4))
                            .executes((ctx) ->
                            {
                                loadConfig();
                                ctx.getSource().sendSuccess(() -> Component.literal("Config reloaded"), false);
                                return 1;
                            })
                            .then(argument("--force-update", StringArgumentType.greedyString())
                                    .executes(ctx ->
                                    {
                                        var input = StringArgumentType.getString(ctx, "--force-update");
                                        if(!input.equalsIgnoreCase("--force-update"))
                                        {
                                            ctx.getSource().sendFailure(Component.literal("Unknown argument passed in! " +
                                                    "Use '/reloadserverlinks --force-update' to send all players an updated list after reloading")
                                                    .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/reloadserverlinks --force-update"))));
                                            return 1;
                                        }
                                        loadConfig();
                                        SendPacketsToPlayers(ctx.getSource().getServer().getPlayerList().getPlayers());
                                        ctx.getSource().sendSuccess(() -> Component.literal("Config reloaded and links distributed to all online players"), false);
                                        return 1;
                                    }))
            );

        });

        ServerLifecycleEvents.SERVER_STARTED.register(s ->
        {

            if(loadConfig())
            {
                AtomicInteger i = new AtomicInteger();
                s.addTickable(() ->
                {
                    if(CONFIG.linkRefreshTime == -1)
                    {
                        return;
                    }
                    if(i.get() >= CONFIG.linkRefreshTime)
                    {
                        var players = s.getPlayerList().getPlayers();
                        SendPacketsToPlayers(players);
                        i.set(0);
                    }
                    else
                    {
                        i.getAndIncrement();
                    }
                });
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                handler.send(new ClientboundServerLinksPacket(CONFIG.buildPacketContents(handler.getPlayer()))));
    }

    private void SendPacketsToPlayers(List<ServerPlayer> players) {
        executor.execute(() -> players.forEach(x -> x.connection.send(new ClientboundServerLinksPacket(CONFIG.buildPacketContents(x)))));
    }

    public static boolean loadConfig()
    {
        try
        {
            var configDir =  FabricLoader.getInstance().getConfigDir().resolve("serverlinkssender");

            BaseConfig config;

            var configFile = configDir.resolve("config.json");
            if (Files.exists(configFile))
            {
                var data = JsonParser.parseString(Files.readString(configFile));

                config = BaseConfig.GSON.fromJson(data, BaseConfig.class);

            }
            else
            {
                Files.createDirectories(configDir);
                Files.createFile(configFile);
                config = new BaseConfig();
            }

            Files.writeString(configFile, BaseConfig.GSON.toJson(config));
            config.preParseText();
            CONFIG = config;
        }
        catch(Throwable exception)
        {
            LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
            return false;
        }

        return true;
    }
}
