package network.ycc.waterdog.pe.packet;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import lombok.NoArgsConstructor;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class PEResourcePackResponse extends DefinedPacket {
    public static final byte STATUS_REFUSED = 1;
    public static final byte STATUS_SEND_PACKS = 2;
    public static final byte STATUS_HAVE_ALL_PACKS = 3;
    public static final byte STATUS_COMPLETED = 4;

    private int status;
    private String[] packs = new String[0];

    public PEResourcePackResponse(int status) {
        this(status, null);
    }

    @Override
    public void read(ByteBuf buf) {
        status = buf.readUnsignedByte();
        final int entryCount = buf.readShortLE();
        final ArrayList<String> packList = new ArrayList<>();
        for (int i = 0 ; i < entryCount ; i++) {
            packList.add(readString(buf));
        }
        packs = packList.toArray(packs);
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeByte(status);
        if (packs == null) {
            buf.writeShortLE(0);
        } else {
            buf.writeShortLE(packs.length);
            for (String pack : packs) {
                writeString(pack, buf);
            }
        }
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        handler.handle(this);
    }
}
