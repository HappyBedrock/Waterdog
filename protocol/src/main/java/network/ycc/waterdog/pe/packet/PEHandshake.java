package network.ycc.waterdog.pe.packet;

import com.google.gson.JsonObject;

import io.netty.buffer.ByteBuf;

import lombok.Data;
import lombok.EqualsAndHashCode;

import net.md_5.bungee.protocol.packet.Handshake;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
public class PEHandshake extends Handshake {
    private String username;
    private UUID uuid;
    private UUID loginUUID;
    private BigInteger xuid;
    private boolean authorized;
    private JsonObject clientInfo;
    private byte[] handshakeData;
    private PublicKey publicKey;

    public PEHandshake() {
        setProtocolVersion(2);
    }

    public PEHandshake(int protocolVersion, String host, int port, String username, UUID uuid,
            boolean authorized, JsonObject clientInfo, BigInteger xuid) {
        super(protocolVersion, host, port, 2);
        this.username = username;
        this.uuid = uuid;
        this.authorized = authorized;
        this.clientInfo = clientInfo;
        this.xuid = xuid;
    }

    @Override
    public void read(ByteBuf buf) {
        handshakeData = new byte[buf.readableBytes()];
        buf.readBytes(handshakeData);
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeBytes(handshakeData);
    }

}
