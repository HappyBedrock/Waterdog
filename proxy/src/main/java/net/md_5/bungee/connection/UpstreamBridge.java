package net.md_5.bungee.connection;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import io.github.waterfallmc.waterfall.StringUtil;
import io.netty.channel.Channel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.ServerConnection.KeepAliveData;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.SettingsChangedEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.forge.ForgeConstants;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PluginMessage;
import net.md_5.bungee.protocol.packet.TabCompleteRequest;
import net.md_5.bungee.protocol.packet.TabCompleteResponse;

public class UpstreamBridge extends PacketHandler
{

    private final ProxyServer bungee;
    private final UserConnection con;

    private long lastTabCompletion = -1;

    public UpstreamBridge(ProxyServer bungee, UserConnection con)
    {
        this.bungee = bungee;
        this.con = con;

        BungeeCord.getInstance().addConnection( con );
        con.getTabListHandler().onConnect();
        con.unsafe().sendPacket( BungeeCord.getInstance().registerChannels( con.getPendingConnection().getVersion() ) );
    }

    @Override
    public void exception(Throwable t) throws Exception
    {
        con.disconnect( Util.exception( t ) );
    }

    @Override
    public void disconnected(ChannelWrapper channel) throws Exception
    {
        // We lost connection to the client
        PlayerDisconnectEvent event = new PlayerDisconnectEvent( con );
        bungee.getPluginManager().callEvent( event );
        con.getTabListHandler().onDisconnect();
        BungeeCord.getInstance().removeConnection( con );

        if ( con.getServer() != null )
        {
            // Manually remove from everyone's tab list
            // since the packet from the server arrives
            // too late
            // TODO: This should only done with server_unique
            //       tab list (which is the only one supported
            //       currently)
            PlayerListItem packet = new PlayerListItem();
            packet.setAction( PlayerListItem.Action.REMOVE_PLAYER );
            PlayerListItem.Item item = new PlayerListItem.Item();
            item.setUuid( con.getUniqueId() );
            packet.setItems( new PlayerListItem.Item[]
            {
                item
            } );
            for ( ProxiedPlayer player : con.getServer().getInfo().getPlayers() )
            {
                // Travertine start
                if ( ProtocolConstants.isAfterOrEq( player.getPendingConnection().getVersion(), ProtocolConstants.MINECRAFT_1_8 ) )
                {
                    player.unsafe().sendPacket( packet );
                }
                // Travertine end
            }
            con.getServer().disconnect( "Quitting" );
        }
    }

    @Override
    public void writabilityChanged(ChannelWrapper channel) throws Exception
    {
        if ( con.getServer() != null )
        {
            Channel server = con.getServer().getCh().getHandle();
            // Waterdog start
            if (network.ycc.waterdog.pe.PEDimSwitchLock.isChannelLocked(channel.getHandle())) {
                server.config().setAutoRead(false);
            } else
            // Waterdog end
            if ( channel.getHandle().isWritable() )
            {
                server.config().setAutoRead( true );
            } else
            {
                server.config().setAutoRead( false );
            }
        }
    }

    @Override
    public boolean shouldHandle(PacketWrapper packet) throws Exception
    {
        return con.getServer() != null || packet.packet instanceof PluginMessage;
    }

    @Override
    public void handle(PacketWrapper packet) throws Exception
    {
        if ( con.getServer() != null )
        {
            con.getEntityRewrite().rewriteServerbound( packet.buf, con.getClientEntityId(), con.getServerEntityId(), con.getPendingConnection().getVersion() );
            // Waterdog start
            if (ProtocolConstants.isPE(con.getPendingConnection().getVersion())) {
                if (con.getEntityRewrite() instanceof net.md_5.bungee.entitymap.EntityMap_PE) {
                    net.md_5.bungee.entitymap.EntityMap_PE entityMap = (net.md_5.bungee.entitymap.EntityMap_PE) con.getEntityRewrite();
                    java.util.Collection<java.util.UUID> playerList = con.getPePlayerList();

                    entityMap.playerListTrackRewrite(packet.buf, playerList::add, playerList::remove,
                            con.getUniqueId(), con.getPendingConnection().getLoginId());
                }
                //TODO: block rewrite?
            }
            // Waterdog end
            con.getServer().getCh().write( packet );
        }
    }

    @Override
    public void handle(KeepAlive alive) throws Exception
    {
        KeepAliveData keepAliveData = con.getServer().getKeepAlives().poll();

        // Waterdog start
        if (ProtocolConstants.isPE(con.getPendingConnection().getVersion())) {
            int newPing = (int) (alive.getRandomId() / 1000000);
            con.getTabListHandler().onPingChange( newPing );
            con.setPing( newPing );
            if (con.getServer() != null && con.getServer().getInfo().isRakNet()) {
                throw CancelSendSignal.INSTANCE; //only forward for TCP servers
            }
            return;
        }
        // Waterdog end
        if ( keepAliveData != null && alive.getRandomId() == keepAliveData.getId() )
        {
            int newPing = (int) ( System.currentTimeMillis() - keepAliveData.getTime() );
            con.getTabListHandler().onPingChange( newPing );
            con.setPing( newPing );
        } else
        {
            throw CancelSendSignal.INSTANCE;
        }
    }

    // Waterdog start
    @Override
    public void handle(network.ycc.waterdog.pe.packet.PEResourcePackResponse command) throws Exception
    {
        throw CancelSendSignal.INSTANCE;
    }
    // Waterdog end

    // Waterdog start
    @Override
    public void handle(network.ycc.waterdog.pe.packet.PECommand command) throws Exception
    {
        int maxLength = 256;
        Preconditions.checkArgument( command.getMessage().length() <= maxLength, "PECommand message too long" ); // Mojang limit, check on updates
        Preconditions.checkArgument(!StringUtil.isBlank(command.getMessage()), "PECommand message is empty");

        ChatEvent chatEvent = new ChatEvent( con, con.getServer(), command.getMessage() );
        if ( !bungee.getPluginManager().callEvent( chatEvent ).isCancelled() )
        {
            command.setMessage( chatEvent.getMessage() );
            if ( !bungee.getPluginManager().dispatchCommand( con, command.getMessage().substring( 1 ) ) )
            {
                con.getServer().unsafe().sendPacket( command );
            }
        }
        throw CancelSendSignal.INSTANCE;
    }
    // Waterdog end

    @Override
    public void handle(Chat chat) throws Exception
    {
        int maxLength = ( con.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_11 ) ? 256 : 100;
        if (ProtocolConstants.isPE(con.getPendingConnection().getVersion())) maxLength = 1024; // Waterdog
        Preconditions.checkArgument( chat.getMessage().length() <= maxLength, "Chat message too long" ); // Mojang limit, check on updates
        if (!ProtocolConstants.isPE(con.getPendingConnection().getVersion())) // Waterdog
        Preconditions.checkArgument(!StringUtil.isBlank(chat.getMessage()), "Chat message is empty");

        ChatEvent chatEvent = new ChatEvent( con, con.getServer(), chat.getMessage() );
        if ( !bungee.getPluginManager().callEvent( chatEvent ).isCancelled() )
        {
            chat.setMessage( chatEvent.getMessage() );
            if ( !chatEvent.isCommand() || !bungee.getPluginManager().dispatchCommand( con, chat.getMessage().substring( 1 ) ) )
            {
                con.getServer().unsafe().sendPacket( chat );
            }
        }
        throw CancelSendSignal.INSTANCE;
    }

    @Override
    public void handle(TabCompleteRequest tabComplete) throws Exception
    {
        // Waterfall start - tab limiter
        if ( bungee.getConfig().getTabThrottle() > 0 &&
                ( con.getPendingConnection().getVersion() >= ProtocolConstants.MINECRAFT_1_13
                && !bungee.getConfig().isDisableModernTabLimiter()))
        {
            long now = System.currentTimeMillis();
            if ( lastTabCompletion > 0 && (now - lastTabCompletion) <= bungee.getConfig().getTabThrottle() )
            {
                throw CancelSendSignal.INSTANCE;
            }
            lastTabCompletion = now;
        }

        // Waterfall end - tab limiter
        List<String> suggestions = new ArrayList<>();

        if ( tabComplete.getCursor().startsWith( "/" ) )
        {
            bungee.getPluginManager().dispatchCommand( con, tabComplete.getCursor().substring( 1 ), suggestions );
        }

        TabCompleteEvent tabCompleteEvent = new TabCompleteEvent( con, con.getServer(), tabComplete.getCursor(), suggestions );
        bungee.getPluginManager().callEvent( tabCompleteEvent );

        if ( tabCompleteEvent.isCancelled() )
        {
            throw CancelSendSignal.INSTANCE;
        }

        List<String> results = tabCompleteEvent.getSuggestions();
        if ( !results.isEmpty() )
        {
            // Unclear how to handle 1.13 commands at this point. Because we don't inject into the command packets we are unlikely to get this far unless
            // Bungee plugins are adding results for commands they don't own anyway
            if ( con.getPendingConnection().getVersion() < ProtocolConstants.MINECRAFT_1_13 )
            {
                con.unsafe().sendPacket( new TabCompleteResponse( results ) );
            } else
            {
                int start = tabComplete.getCursor().lastIndexOf( ' ' ) + 1;
                int end = tabComplete.getCursor().length();
                StringRange range = StringRange.between( start, end );

                List<Suggestion> brigadier = new LinkedList<>();
                for ( String s : results )
                {
                    brigadier.add( new Suggestion( range, s ) );
                }

                con.unsafe().sendPacket( new TabCompleteResponse( tabComplete.getTransactionId(), new Suggestions( range, brigadier ) ) );
            }
            throw CancelSendSignal.INSTANCE;
        }
    }

    @Override
    public void handle(ClientSettings settings) throws Exception
    {
        con.setSettings( settings );

        SettingsChangedEvent settingsEvent = new SettingsChangedEvent( con );
        bungee.getPluginManager().callEvent( settingsEvent );
    }

    @Override
    public void handle(PluginMessage pluginMessage) throws Exception
    {
        if ( pluginMessage.getTag().equals( "BungeeCord" ) )
        {
            throw CancelSendSignal.INSTANCE;
        }

        if ( BungeeCord.getInstance().config.isForgeSupport() )
        {
            // Hack around Forge race conditions
            if ( pluginMessage.getTag().equals( "FML" ) && pluginMessage.getStream().readUnsignedByte() == 1 )
            {
                throw CancelSendSignal.INSTANCE;
            }

            // We handle forge handshake messages if forge support is enabled.
            if ( pluginMessage.getTag().equals( ForgeConstants.FML_HANDSHAKE_TAG ) )
            {
                // Let our forge client handler deal with this packet.
                con.getForgeClientHandler().handle( pluginMessage );
                throw CancelSendSignal.INSTANCE;
            }

            if ( con.getServer() != null && !con.getServer().isForgeServer() && pluginMessage.getData().length > Short.MAX_VALUE )
            {
                // Drop the packet if the server is not a Forge server and the message was > 32kiB (as suggested by @jk-5)
                // Do this AFTER the mod list, so we get that even if the intial server isn't modded.
                throw CancelSendSignal.INSTANCE;
            }
        }

        PluginMessageEvent event = new PluginMessageEvent( con, con.getServer(), pluginMessage.getTag(), pluginMessage.getData().clone() );
        if ( bungee.getPluginManager().callEvent( event ).isCancelled() )
        {
            throw CancelSendSignal.INSTANCE;
        }

        // TODO: Unregister as well?
        if ( PluginMessage.SHOULD_RELAY.apply( pluginMessage ) )
        {
            con.getPendingConnection().getRelayMessages().add( pluginMessage );
        }
    }

    @Override
    public String toString()
    {
        return "[" + con.getAddress() + "|" + con.getName() + "] -> UpstreamBridge";
    }
}