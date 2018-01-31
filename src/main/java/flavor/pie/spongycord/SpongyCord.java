package flavor.pie.spongycord;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

@Plugin(id="spongycord",name="SpongyCord",version="1.1.1",authors="pie_flavor",description="A simple wrapper API for Bungee.")
public class SpongyCord {
    @Inject
    Game game;
    Task task;
    private static SpongyCord instance;
    @Listener
    public void preInit(GamePreInitializationEvent e) {
        instance = this;
    }
    @Listener
    public void startingServer(GameStartingServerEvent e) {
        API.channel = game.getChannelRegistrar().getOrCreateRaw(this, "BungeeCord");
        API.channel.addListener(Platform.Type.SERVER, API.listener = new API.ChannelListener());
    }
    @Listener
    public void stoppingServer(GameStoppingServerEvent e) {
        game.getChannelRegistrar().unbindChannel(API.channel);
    }

    /**
     * The API for SpongyCord.
     *
     * @author pie_flavor (Adam Spofford)
     */
    public static class API {
        private static ChannelBinding.RawDataChannel channel;
        private static ChannelListener listener;

        private static void checkChannel() {
            if (channel == null) {
                throw new IllegalStateException("The message channel could not be found!");
            }
        }
        /**
         * Connects a player to another server.
         *
         * @param player The player to connect
         * @param server The server to connect to
         */
        public static void connectPlayer(Player player, String server) {
            checkChannel();
            checkNotNull(player);
            checkNotNull(server);
            channel.sendTo(player, buf -> buf.writeUTF("Connect").writeUTF(server));
        }

        /**
         * Connects a player to another server. The player may or may not be on the current server.
         *
         * @param player The name of the player to connect
         * @param server The server to connect the player to
         * @param reference Any player
         */
        public static void connectPlayer(String player, String server, Player reference) {
            checkChannel();
            checkNotNull(player);
            checkNotNull(server);
            checkNotNull(reference);
            channel.sendTo(reference, buf -> buf.writeUTF("ConnectOther").writeUTF(player).writeUTF(server));
        }

        /**
         * Gets the real {@link InetSocketAddress} of a player.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link Consumer} is used as a listener.
         *
         * @param player The player to get the IP from.
         * @param consumer A consumer which will be called when the address arrives.
         */
        public static void getIP(Player player, Consumer<InetSocketAddress> consumer) {
            checkChannel();
            checkNotNull(player);
            checkNotNull(consumer);
            channel.sendTo(player, buf -> buf.writeUTF("IP"));
            addListener(
                    buf -> buf.resetRead().readUTF().equals("IP"),
                    buf -> consumer.accept(new InetSocketAddress(buf.readUTF(), buf.readInteger()))
            );
        }

        /**
         * Gets the number of players connected to a server.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link IntConsumer} is used as a listener.
         *
         * @param server The server to get the player count from
         * @param consumer A consumer which will be called when the player count arrives
         * @param reference Any player
         */
        public static void getPlayerCount(String server, IntConsumer consumer, Player reference) {
            checkChannel();
            checkNotNull(reference);
            checkNotNull(consumer);
            checkNotNull(server);
            channel.sendTo(reference, buf -> buf.writeUTF("PlayerCount").writeUTF(server));
            addListener(
                    buf -> buf.resetRead().readUTF().equals("PlayerCount") && buf.readUTF().equals(server),
                    buf -> consumer.accept(buf.readInteger())
            );
        }

        /**
         * Gets the number of players connected to the server network.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link IntConsumer} is used as a listener.
         *
         * @param consumer A consumer which will be called when the player count arrives
         * @param reference Any player
         */
        public static void getGlobalPlayerCount(IntConsumer consumer, Player reference) {
            getPlayerCount("ALL", consumer, reference);
        }

        /**
         * Lists players connected to a server.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link Consumer} is used as a listener.
         *
         * @param server The server to get the list from
         * @param consumer A consumer which will be called when the list arrives
         * @param reference Any player
         */
        public static void listPlayers(String server, Consumer<List<String>> consumer, Player reference) {
            checkChannel();
            checkNotNull(server);
            checkNotNull(reference);
            channel.sendTo(reference, buf->buf.writeUTF("PlayerList").writeUTF(server));
            addListener(
                    buf -> buf.resetRead().readUTF().equals("PlayerList") && buf.readUTF().equals(server),
                    buf -> consumer.accept(ImmutableList.<String>builder().add(buf.readUTF().split(", ")).build())
            );
        }

        /**
         * Lists players connected to the server network.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link Consumer} is used as a listener.
         *
         * @param consumer A consumer which will be called when the list arrives
         * @param reference Any player
         */
        public static void listAllPlayers(Consumer<List<String>> consumer, Player reference) {
            listPlayers("ALL", consumer, reference);
        }

        /**
         * Lists all servers in the network.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link Consumer} is used as a listener.
         *
         * @param consumer A consumer which will be called when the list arrives
         * @param reference Any player
         */
        public static void getServerList(Consumer<List<String>> consumer, Player reference) {
            checkChannel();
            checkNotNull(consumer);
            checkNotNull(reference);
            channel.sendTo(reference, buf -> buf.writeUTF("GetServers"));
            addListener(
                    buf -> buf.resetRead().readUTF().equals("GetServers"),
                    buf -> consumer.accept(ImmutableList.<String>builder().add(buf.readUTF().split(", ")).build())
            );
        }

        /**
         * Sends a message to a player. The player may or may not be on this server.
         *
         * @param player The player to send the message to
         * @param message The message to send
         * @param reference Any player
         */
        public static void sendMessage(String player, Text message, Player reference) {
            checkChannel();
            checkNotNull(reference);
            checkNotNull(message);
            checkNotNull(player);
            channel.sendTo(reference, buf -> buf.writeUTF("Message").writeUTF(player).writeUTF(TextSerializers.LEGACY_FORMATTING_CODE.serialize(message)));
        }

        /**
         * Gets the Bungee-defined name of this server.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link Consumer} is used as a listener.
         *
         * @param consumer A consumer that will be called when the name arrives
         * @param reference Any player
         */
        public static void getServerName(Consumer<String> consumer, Player reference) {
            checkChannel();
            checkNotNull(consumer);
            checkNotNull(reference);
            channel.sendTo(reference, buf -> buf.writeUTF("GetServer"));
            addListener(
                    buf -> buf.resetRead().readUTF().equals("GetServer"),
                    buf -> consumer.accept(buf.readUTF())
            );
        }

        /**
         * Sends a plugin message to the specified server.
         *
         * @param message The message to be sent
         * @param channel The channel to send it on
         * @param server The server to send it to
         * @param reference Any player
         */
        public static void sendServerPluginMessage(byte[] message, String channel, String server, Player reference) {
            checkChannel();
            checkNotNull(message);
            checkNotNull(channel);
            checkNotNull(server);
            checkNotNull(reference);
            API.channel.sendTo(reference, buf -> {
                buf.writeUTF("Forward").writeUTF(server).writeUTF(channel).writeShort((short) message.length);
                for (byte b : message) {
                    buf.writeByte(b);
                }
            });
        }

        /**
         * Sends a plugin message to all servers.
         *
         * @param message The message to be sent
         * @param channel The channel to send it on
         * @param reference Any player
         */
        public static void sendGlobalPluginMessage(byte[] message, String channel, Player reference) {
            sendServerPluginMessage(message, channel, "ALL", reference);
        }

        /**
         * Sends a plugin message to a player. The player may or may not be on this server.
         *
         * @param message The message to send
         * @param channel The channel to send it on
         * @param player The player to send it to
         * @param reference Any player
         */
        public static void sendPlayerPluginMessage(byte[] message, String channel, String player, Player reference) {
            checkChannel();
            checkNotNull(message);
            checkNotNull(channel);
            checkNotNull(player);
            checkNotNull(reference);
            API.channel.sendTo(reference, buf -> {
                buf.writeUTF("ForwardToPlayer").writeUTF(player).writeUTF(channel).writeShort((short) message.length);
                for (byte b : message) {
                    buf.writeByte(b);
                }
            });
        }

        /**
         * Gets the real {@link UUID} of a player.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link Consumer} is used as a listener.
         *
         * @param player The player to get the {@link UUID} of.
         * @param consumer A consumer that will be called when the {@link UUID} arrives.
         */
        public static void getRealUUID(Player player, Consumer<UUID> consumer) {
            checkChannel();
            checkNotNull(player);
            checkNotNull(consumer);
            channel.sendTo(player, buf -> buf.writeUTF("UUID"));
            addListener(
                    buf -> buf.resetRead().readUTF().equals("UUID"),
                    buf -> consumer.accept(UUID.fromString(buf.readUTF()))
            );
        }

        /**
         * Gets the real {@link UUID} of a player. The player may or may not be on this server.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link Consumer} is used as a listener.
         *
         * @param player The player to get the {@link UUID} of
         * @param consumer A consumer that will be called when the {@link UUID} arrives
         * @param reference Any player
         */
        public static void getRealUUID(String player, Consumer<UUID> consumer, Player reference) {
            checkChannel();
            checkNotNull(player);
            checkNotNull(consumer);
            checkNotNull(reference);
            channel.sendTo(reference, buf -> buf.writeUTF("UUIDOther").writeUTF(player));
            addListener(
                    buf -> buf.resetRead().readUTF().equals("UUIDOther") && buf.readUTF().equals(player),
                    buf -> consumer.accept(UUID.fromString(buf.readUTF()))
            );
        }

        /**
         * Gets the {@link InetSocketAddress} of a server.
         * This method depends on an incoming message, so it does not return per se; rather, the {@link Consumer} is used as a listener.
         *
         * @param server The server to get it from
         * @param consumer A consumer that will be called when the {@link InetSocketAddress} arrives
         * @param reference Any player
         */
        public static void getServerIP(String server, Consumer<InetSocketAddress> consumer, Player reference) {
            checkChannel();
            checkNotNull(consumer);
            checkNotNull(reference);
            channel.sendTo(reference, buf -> buf.writeUTF("ServerIP").writeUTF(server));
            addListener(
                    buf -> buf.resetRead().readUTF().equals("ServerIP") && buf.readUTF().equals(server),
                    buf -> consumer.accept(new InetSocketAddress(buf.readUTF(), buf.readShort()))
            );
        }
        
        /**
         * Kicks a player off the network with the specified message.
         *
         * @param player The player to kick
         * @param reason The reason that they were kicked for
         * @param reference Any player
         */
        public static void kickPlayer(String player, Text reason, Player reference) {
            checkChannel();
            checkNotNull(player);
            checkNotNull(reason);
            checkNotNull(reference);
            channel.sendTo(reference, buf -> buf.writeUTF("KickPlayer").writeUTF(player).writeUTF(TextSerializers.LEGACY_FORMATTING_CODE.serialize(reason)));
        }
        private static void addListener(Predicate<ChannelBuf> predicate, Consumer<ChannelBuf> consumer) {
            listener.map.put(predicate, consumer);
        }
        private static class ChannelListener implements RawDataListener {
            ConcurrentMap<Predicate<ChannelBuf>, Consumer<ChannelBuf>> map = Maps.newConcurrentMap();
            @Override
            public void handlePayload(ChannelBuf data, RemoteConnection connection, Platform.Type side) {
                for (Map.Entry<Predicate<ChannelBuf>, Consumer<ChannelBuf>> entry : map.entrySet()) {
                   if (entry.getKey().test(data)) {
                       entry.getValue().accept(data);
                       map.remove(entry.getKey());
                       return;
                   }
                }
            }
        }
    }
}
