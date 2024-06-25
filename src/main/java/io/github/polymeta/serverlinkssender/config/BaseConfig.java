package io.github.polymeta.serverlinkssender.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.mojang.datafixers.util.Either;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.predicate.api.GsonPredicateSerializer;
import eu.pb4.predicate.api.MinecraftPredicate;
import eu.pb4.predicate.api.PredicateContext;
import eu.pb4.predicate.impl.predicates.compat.PermissionPredicate;
import io.github.polymeta.serverlinkssender.ServerLinksSender;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseConfig
{
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setLenient()
            .registerTypeHierarchyAdapter(MinecraftPredicate.class, GsonPredicateSerializer.INSTANCE)
            .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT, java.lang.reflect.Modifier.STATIC)
            .create();

    @SerializedName("refresh_interval")
    public int linkRefreshTime = -1;

    @SerializedName("global_server_links")
    private Map<String, String> globalLinks = Map.of("<red><bold>My cool link!", "https://google.com");
    public transient Map<TextNode, String> parsedGlobalLinks;

    @SerializedName("additional_links")
    public List<ContextLinkEntry> additionalLinks = List.of(new ContextLinkEntry());

    public void preParseText()
    {
        parsedGlobalLinks = globalLinks.entrySet()
                .stream()
                .collect(Collectors.toMap(x -> ServerLinksSender.PARSER.parseNode(x.getKey()), Map.Entry::getValue));
        additionalLinks.forEach(x ->
                x.parsedLinks = x.links.entrySet()
                        .stream()
                        .collect(Collectors.toMap(y -> ServerLinksSender.PARSER.parseNode(y.getKey()), Map.Entry::getValue)));
    }

    public List<ServerLinks.UntrustedEntry> buildPacketContents(ServerPlayer player)
    {
        var links = parsedGlobalLinks.entrySet().stream()
                .map(x -> new ServerLinks.UntrustedEntry(Either.right(x.getKey().toText(PlaceholderContext.of(player))), x.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
        links.addAll(additionalLinks.stream()
                .filter(x -> x.requirement.test(PredicateContext.of(player)).success())
                .flatMap(x -> x.parsedLinks.entrySet().stream())
                .map(x -> new ServerLinks.UntrustedEntry(Either.right(x.getKey().toText(PlaceholderContext.of(player))), x.getValue()))
                .collect(Collectors.toCollection(ArrayList::new)));
        return links;
    }

    public static class ContextLinkEntry
    {
        private Map<String, String> links = Map.of("<rb>My cool extra link!", "https://google.com");
        public transient Map<TextNode, String> parsedLinks;
        public MinecraftPredicate requirement = new PermissionPredicate("example.permission", 2);
    }
}
