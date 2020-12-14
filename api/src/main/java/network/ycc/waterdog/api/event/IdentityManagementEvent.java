package network.ycc.waterdog.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import java.util.UUID;

@Data
@ToString(callSuper = false)
@EqualsAndHashCode(callSuper = false)
public class IdentityManagementEvent extends AsyncEvent<IdentityManagementEvent> {
    private UUID uuid;
    private String username;

    public IdentityManagementEvent(UUID uuid, String username, Callback<IdentityManagementEvent> done) {
        super(done);
        this.uuid = uuid;
        this.username = username;
    }
}
