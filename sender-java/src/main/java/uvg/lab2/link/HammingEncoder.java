package uvg.lab2.link;

/**
 * Codificador Hamming(12,8) con paridad par, implementado manualmente.
 * <p>
 * Posiciones (1-indexadas) dentro de cada bloque de 12 bits:
 * paridad: 1, 2, 4, 8
 * datos:   3, 5, 6, 7, 9, 10, 11, 12
 * <p>
 * Cada byte de 8 bits produce un bloque de 12 bits. Hamming(12,8) sin
 * paridad global corrige un único bit erróneo por bloque, pero no
 * garantiza la detección de errores múltiples dentro del mismo bloque.
 */
public final class HammingEncoder implements IntegrityEncoder {

    static final int[] DATA_POSITIONS = {3, 5, 6, 7, 9, 10, 11, 12};
    static final int[] PARITY_POSITIONS = {1, 2, 4, 8};

    @Override
    public String encode(String dataBits) {
        if (dataBits.length() % 8 != 0) {
            throw new IllegalArgumentException("Hamming requiere múltiplos de 8 bits");
        }
        StringBuilder out = new StringBuilder();
        for (int offset = 0; offset < dataBits.length(); offset += 8) {
            out.append(encodeBlock(dataBits.substring(offset, offset + 8)));
        }
        return out.toString();
    }

    private String encodeBlock(String byteBits) {
        int[] block = new int[13]; // índice 1..12
        for (int i = 0; i < DATA_POSITIONS.length; i++) {
            block[DATA_POSITIONS[i]] = byteBits.charAt(i) - '0';
        }
        for (int p : PARITY_POSITIONS) {
            int parity = 0;
            for (int i = 1; i <= 12; i++) {
                if (i != p && (i & p) != 0) {
                    parity ^= block[i];
                }
            }
            block[p] = parity;
        }
        StringBuilder sb = new StringBuilder(12);
        for (int i = 1; i <= 12; i++) {
            sb.append(block[i]);
        }
        return sb.toString();
    }

    @Override
    public String algorithmName() {
        return "HAMMING_12_8";
    }
}
