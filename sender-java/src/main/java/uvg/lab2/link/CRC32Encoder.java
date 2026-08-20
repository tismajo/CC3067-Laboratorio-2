package uvg.lab2.link;

/**
 * Codificador CRC-32/IEEE (CRC-32/ISO-HDLC) implementado manualmente,
 * sin usar java.util.zip.CRC32.
 * <p>
 * Polinomio reflejado: 0xEDB88320, valor inicial 0xFFFFFFFF,
 * RefIn=true, RefOut=true, XorOut=0xFFFFFFFF.
 * Formato de salida: [bits de datos][32 bits de CRC MSB-first].
 */
public final class CRC32Encoder implements IntegrityEncoder {

    private static final long POLY = 0xEDB88320L;

    @Override
    public String encode(String dataBits) {
        if (dataBits.length() % 8 != 0) {
            throw new IllegalArgumentException("CRC-32 requiere múltiplos de 8 bits");
        }
        long crc = compute(dataBits);
        return dataBits + toBits32(crc);
    }

    /** Calcula el CRC-32/IEEE de una cadena de bits (múltiplo de 8) y retorna el valor de 32 bits. */
    public static long compute(String dataBits) {
        byte[] bytes = bitsToBytes(dataBits);
        long crc = 0xFFFFFFFFL;
        for (byte b : bytes) {
            crc ^= (b & 0xFFL);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1L) != 0L) {
                    crc = (crc >>> 1) ^ POLY;
                } else {
                    crc = crc >>> 1;
                }
            }
        }
        return (crc ^ 0xFFFFFFFFL) & 0xFFFFFFFFL;
    }

    private static byte[] bitsToBytes(String bits) {
        byte[] bytes = new byte[bits.length() / 8];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(bits.substring(i * 8, i * 8 + 8), 2);
        }
        return bytes;
    }

    private static String toBits32(long value) {
        StringBuilder sb = new StringBuilder(32);
        for (int bit = 31; bit >= 0; bit--) {
            sb.append((value >> bit) & 1L);
        }
        return sb.toString();
    }

    @Override
    public String algorithmName() {
        return "CRC32_IEEE";
    }
}
