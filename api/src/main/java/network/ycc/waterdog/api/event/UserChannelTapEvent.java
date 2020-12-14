package network.ycc.waterdog.api.event;

import io.netty.channel.Channel;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import net.md_5.bungee.api.plugin.Event;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserChannelTapEvent extends Event {
    @Getter
    private Channel channel;
}
