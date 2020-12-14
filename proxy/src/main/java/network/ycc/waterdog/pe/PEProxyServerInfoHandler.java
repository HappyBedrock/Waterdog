package network.ycc.waterdog.pe;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.protocol.ProtocolConstants;

import network.ycc.raknet.RakNet;
import network.ycc.raknet.packet.Packet;
import network.ycc.raknet.packet.UnconnectedPing;
import network.ycc.raknet.packet.UnconnectedPong;
import network.ycc.raknet.server.pipeline.UdpPacketHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.logging.Level;

public class PEProxyServerInfoHandler extends UdpPacketHandler<UnconnectedPing> {

    protected final ListenerInfo listenerInfo;
    protected final ProxyServer bungee;

    public PEProxyServerInfoHandler(ProxyServer bungee, ListenerInfo listenerInfo) {
        super(UnconnectedPing.class);
        this.listenerInfo = listenerInfo;
        this.bungee = bungee;
    }

    protected void handle(ChannelHandlerContext ctx, InetSocketAddress sender, UnconnectedPing ping) {
        final RakNet.Config config = RakNet.config(ctx);
        final long clientTime = ping.getClientTime(); //must ditch references to ping
        final ServerPing.Protocol protocol = new ServerPing.Protocol(
                "", //leave version blank, we do multi-version.
                ProtocolConstants.MINECRAFT_PE_1_11 + ProtocolConstants.PE_PROTOCOL_OFFSET
        );
        final ServerPing.Players players = new ServerPing.Players(
                listenerInfo.getMaxPlayers(), bungee.getOnlineCount(), new ServerPing.PlayerInfo[0]
        );
        final BaseComponent desc = new TextComponent(TextComponent.fromLegacyText(listenerInfo.getMotd().trim()));
        final ServerPing serverPing = new ServerPing(protocol, players, desc, null);
        final ProxyPingEvent ev = new ProxyPingEvent(new PingConnection(sender), serverPing, (event, throwable) -> {
            final String response;
            if (throwable != null) {
                bungee.getLogger().log(Level.WARNING, "Failed processing PE ping:", throwable);
                response = "";
            } else {
                final ServerPing result = event.getResponse();
                response = String.join(";",
                        "MCPE",
                        result.getDescriptionComponent().toLegacyText().replace(";", "\\;"),
                        String.valueOf(result.getVersion().getProtocol()),
                        result.getVersion().getName(),
                        String.valueOf(result.getPlayers().getOnline()),
                        String.valueOf(result.getPlayers().getMax()),
                        String.valueOf(config.getServerId())
                );
            }
            final Packet pong = new UnconnectedPong(clientTime, config.getServerId(), config.getMagic(), response);
            final ByteBuf tmpBuf = ctx.alloc().ioBuffer(pong.sizeHint());
            try {
                config.getCodec().encode(pong, tmpBuf);
                for (int i = 0 ; i < 3; i++) { //send multiple responses for bad connections
                    ctx.writeAndFlush(new DatagramPacket(tmpBuf.retainedSlice(), sender))
                            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                }
            } finally {
                ReferenceCountUtil.safeRelease(pong);
                tmpBuf.release();
            }
        });
        bungee.getPluginManager().callEvent(ev);
    }

    protected class PingConnection implements PendingConnection {

        final InetSocketAddress remoteAddress;

        PingConnection(InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public String getName() {
            return null;
        }

        public int getVersion() {
            return ProtocolConstants.MINECRAFT_PE_1_12;
        }

        public InetSocketAddress getVirtualHost() {
            return null;
        }

        public ListenerInfo getListener() {
            return listenerInfo;
        }

        public String getUUID() {
            return null;
        }

        public UUID getUniqueId() {
            return null;
        }

        public void setUniqueId(UUID uuid) {}

        public boolean isOnlineMode() {
            return bungee.getConfig().isOnlineMode();
        }

        public void setOnlineMode(boolean b) {}

        public boolean isLegacy() {
            return true;
        }

        public InetSocketAddress getAddress() {
            return remoteAddress;
        }

        public SocketAddress getSocketAddress() {
            return remoteAddress;
        }

        public void disconnect(String s) {}

        public void disconnect(BaseComponent... baseComponents) {}

        public void disconnect(BaseComponent baseComponent) {}

        public boolean isConnected() {
            return false;
        }

        public Unsafe unsafe() {
            return x -> {};
        }

    }

}
