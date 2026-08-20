package uvg.lab2.protocol;

/**
 * Trama enviada por el emisor. Serializa a un objeto JSON de una sola línea,
 * sin depender de librerías externas.
 */
public final class Frame {

    public final int version;
    public final String messageId;
    public final String algorithm;
    public final int originalBitLength;
    public final int encodedBitLength;
    public final double errorProbability;
    public final String payload;

    public Frame(String messageId, String algorithm, int originalBitLength,
                 int encodedBitLength, double errorProbability, String payload) {
        this.version = 1;
        this.messageId = messageId;
        this.algorithm = algorithm;
        this.originalBitLength = originalBitLength;
        this.encodedBitLength = encodedBitLength;
        this.errorProbability = errorProbability;
        this.payload = payload;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"version\":").append(version).append(',');
        sb.append("\"messageId\":\"").append(escape(messageId)).append("\",");
        sb.append("\"algorithm\":\"").append(escape(algorithm)).append("\",");
        sb.append("\"originalBitLength\":").append(originalBitLength).append(',');
        sb.append("\"encodedBitLength\":").append(encodedBitLength).append(',');
        sb.append("\"errorProbability\":").append(errorProbability).append(',');
        sb.append("\"payload\":\"").append(escape(payload)).append('"');
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
