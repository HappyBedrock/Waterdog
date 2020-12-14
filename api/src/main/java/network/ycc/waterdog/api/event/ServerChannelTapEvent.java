package network.ycc.waterdog.api.event;

import io.netty.channel.Channel;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import lombok.Getter;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ServerChannelTapEvent extends Event {
    @Getter
    private ProxiedPlayer user;
    @Getter
    private ServerInfo serverInfo;
    @Getter
    private Channel channel;
}
