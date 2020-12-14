package network.ycc.waterdog.api.metrics;

public interface RakNetMetrics {
    void preCompressionBytes(int i);
    void postCompressionBytes(int i);
    void preCompressionPacket(int i);
}
