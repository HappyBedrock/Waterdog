package network.ycc.waterdog.pe;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.protocol.ProtocolConstants;

public class PEDataValues {
    public static final int CHAT_CLIENT_RAW_TYPE = 0;
    public static final int CHAT_CLIENT_CHAT_TYPE = 1;
    public static final int CHAT_CLIENT_TIP_TYPE = 5;
    public static final int CHAT_CLIENT_SYSTEM_TYPE = 6;

    public static final int PARTICLE_TYPE_TERRAIN = 18;

    public static final int LEVEL_EVENT_EVENT_ADD_PARTICLE_MASK = 0x4000;
    public static final int LEVEL_EVENT_EVENT_TERRAIN_PARTICLE = LEVEL_EVENT_EVENT_ADD_PARTICLE_MASK | PARTICLE_TYPE_TERRAIN;
    public static final int LEVEL_EVENT_EVENT_PARTICLE_PUNCH_BLOCK = 2014;
    public static final int LEVEL_EVENT_EVENT_PARTICLE_DESTROY = 2001;

    public static int getPcChatType(int peId) {
        switch (peId) {
            case CHAT_CLIENT_TIP_TYPE: return ChatMessageType.ACTION_BAR.ordinal();
            case CHAT_CLIENT_SYSTEM_TYPE: return ChatMessageType.SYSTEM.ordinal();
            default: return ChatMessageType.CHAT.ordinal();
        }
    }

    public static int getPeChatType(int pcId, ProtocolConstants.Direction direction) {
        switch (ChatMessageType.values()[pcId]) {
            case ACTION_BAR: return CHAT_CLIENT_TIP_TYPE;
            case SYSTEM: return CHAT_CLIENT_SYSTEM_TYPE;
            default:
                return direction == ProtocolConstants.Direction.TO_SERVER ?
                        CHAT_CLIENT_CHAT_TYPE : CHAT_CLIENT_RAW_TYPE;
        }
    }

    public static int getPeDimensionId(int dimId) {
        switch (dimId) {
            case -1: return 1;
            case 1: return 2;
            case 0: return 0;
            default: throw new IllegalArgumentException("Unknown dim id " + dimId);
        }
    }

    public static int getPcDimensionId(int dimId) {
        switch (dimId) {
            case 1: return -1;
            case 2: return 1;
            case 0: return 0;
            default: throw new IllegalArgumentException("Unknown dim id " + dimId);
        }
    }
}
