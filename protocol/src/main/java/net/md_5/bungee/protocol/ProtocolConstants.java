package net.md_5.bungee.protocol;

import java.util.Arrays;
import java.util.List;

public class ProtocolConstants
{

    // Waterdog start
    public static final int PE_PROTOCOL_OFFSET = 2000;
    public static final int MINECRAFT_PE_1_8 = 313 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_9 = 332 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_10 = 340 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_11 = 354 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_12 = 361 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_13 = 388 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_14 = 389 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_14_HOTFIX = 390 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_15 = 392 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_16 = 407 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_16_20 = 408 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_16_100 = 419 - PE_PROTOCOL_OFFSET;
    public static final int MINECRAFT_PE_1_16_200 = 422 - PE_PROTOCOL_OFFSET;
    // Waterdog end
    public static final int MINECRAFT_1_7_2 = 4;
    public static final int MINECRAFT_1_7_6 = 5;
    public static final int MINECRAFT_1_8 = 47;
    public static final int MINECRAFT_1_9 = 107;
    public static final int MINECRAFT_1_9_1 = 108;
    public static final int MINECRAFT_1_9_2 = 109;
    public static final int MINECRAFT_1_9_4 = 110;
    public static final int MINECRAFT_1_10 = 210;
    public static final int MINECRAFT_1_11 = 315;
    public static final int MINECRAFT_1_11_1 = 316;
    public static final int MINECRAFT_1_12 = 335;
    public static final int MINECRAFT_1_12_1 = 338;
    public static final int MINECRAFT_1_12_2 = 340;
    public static final int MINECRAFT_1_13 = 393;
    public static final int MINECRAFT_1_13_1 = 401;
    public static final int MINECRAFT_1_13_2 = 404;
    public static final int MINECRAFT_1_14 = 477;
    public static final int MINECRAFT_1_14_1 = 480;
    public static final int MINECRAFT_1_14_2 = 485;
    public static final int MINECRAFT_1_14_3 = 490;
    public static final int MINECRAFT_1_14_4 = 498;
    public static final int MINECRAFT_1_15 = 573;
    public static final int MINECRAFT_1_15_1 = 575;
    public static final int MINECRAFT_1_15_2 = 578;
    public static final List<String> SUPPORTED_VERSIONS = Arrays.asList(
            // Waterdog start
            "PE-1.8.x",
            "PE-1.9.x",
            "PE-1.10.x",
            "PE-1.11.x",
            "PE-1.12.x",
            "PE-1.13.x",
            "PE-1.14.x",
            "PE-1.15.x",
            "PE-1.16.x",
            // Waterdog end
            "1.7.x",
            "1.8.x",
            "1.9.x",
            "1.10.x",
            "1.11.x",
            "1.12.x",
            "1.13.x",
            "1.14.x",
            "1.15.x"
    );
    public static final List<Integer> SUPPORTED_VERSION_IDS = Arrays.asList(
            // Waterdog start
            ProtocolConstants.MINECRAFT_PE_1_8,
            ProtocolConstants.MINECRAFT_PE_1_9,
            ProtocolConstants.MINECRAFT_PE_1_10,
            ProtocolConstants.MINECRAFT_PE_1_11,
            ProtocolConstants.MINECRAFT_PE_1_12,
            ProtocolConstants.MINECRAFT_PE_1_13,
            ProtocolConstants.MINECRAFT_PE_1_14,
            ProtocolConstants.MINECRAFT_PE_1_14_HOTFIX,
            ProtocolConstants.MINECRAFT_PE_1_15,
            ProtocolConstants.MINECRAFT_PE_1_16,
            ProtocolConstants.MINECRAFT_PE_1_16_20,
            ProtocolConstants.MINECRAFT_PE_1_16_100,
            ProtocolConstants.MINECRAFT_PE_1_16_200,
            // Waterdog end
            ProtocolConstants.MINECRAFT_1_7_2,
            ProtocolConstants.MINECRAFT_1_7_6,
            ProtocolConstants.MINECRAFT_1_8,
            ProtocolConstants.MINECRAFT_1_9,
            ProtocolConstants.MINECRAFT_1_9_1,
            ProtocolConstants.MINECRAFT_1_9_2,
            ProtocolConstants.MINECRAFT_1_9_4,
            ProtocolConstants.MINECRAFT_1_10,
            ProtocolConstants.MINECRAFT_1_11,
            ProtocolConstants.MINECRAFT_1_11_1,
            ProtocolConstants.MINECRAFT_1_12,
            ProtocolConstants.MINECRAFT_1_12_1,
            ProtocolConstants.MINECRAFT_1_12_2,
            ProtocolConstants.MINECRAFT_1_13,
            ProtocolConstants.MINECRAFT_1_13_1,
            ProtocolConstants.MINECRAFT_1_13_2,
            ProtocolConstants.MINECRAFT_1_14,
            ProtocolConstants.MINECRAFT_1_14_1,
            ProtocolConstants.MINECRAFT_1_14_2,
            ProtocolConstants.MINECRAFT_1_14_3,
            ProtocolConstants.MINECRAFT_1_14_4,
            ProtocolConstants.MINECRAFT_1_15,
            ProtocolConstants.MINECRAFT_1_15_1,
            ProtocolConstants.MINECRAFT_1_15_2
    );

    public static final boolean isBeforeOrEq(int before, int other)
    {
            if (isPE(before) != isPE(other)) return false; // Waterdog - no compare PE
            return before <= other;
    }

    public static final boolean isAfterOrEq(int after, int other)
    {
            if (isPE(after) != isPE(other)) return false; // Waterdog - no compare PE
            return after >= other;
    }

    // Waterdog start
    public static final boolean isPE(int v) {
        return v < -1;
    }
    // Waterdog end

    public enum Direction
    {

        TO_CLIENT, TO_SERVER;
    }
}
