package uvg.lab2.presentation;

/**
 * Capa de presentación: convierte texto ASCII a una cadena de bits MSB-first
 * y viceversa. Cada byte se representa con exactamente 8 caracteres '0'/'1'.
 */
public final class BinaryEncoder {

    private BinaryEncoder() {
    }

    public static String textToBits(String text) {
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        StringBuilder sb = new StringBuilder(bytes.length * 8);
        for (byte b : bytes) {
            int value = b & 0xFF;
            for (int bit = 7; bit >= 0; bit--) {
                sb.append((value >> bit) & 1);
            }
        }
        return sb.toString();
    }

    public static String bitsToText(String bits) {
        if (bits.length() % 8 != 0) {
            throw new IllegalArgumentException("La longitud de bits debe ser múltiplo de 8");
        }
        StringBuilder sb = new StringBuilder(bits.length() / 8);
        for (int i = 0; i < bits.length(); i += 8) {
            int value = Integer.parseInt(bits.substring(i, i + 8), 2);
            if (value > 0x7F) {
                throw new IllegalArgumentException("Byte fuera de rango ASCII: " + value);
            }
            sb.append((char) value);
        }
        return sb.toString();
    }
}
