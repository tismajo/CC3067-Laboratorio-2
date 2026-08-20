package uvg.lab2.noise;

import java.util.Random;

/**
 * Simula ruido de canal invirtiendo cada bit de forma independiente
 * con probabilidad "probability".
 */
public final class NoiseSimulator {

    private final Random random;

    public NoiseSimulator() {
        this.random = new Random();
    }

    public NoiseSimulator(long seed) {
        this.random = new Random(seed);
    }

    public Result apply(String bits, double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException("La probabilidad debe estar entre 0 y 1");
        }
        StringBuilder sb = new StringBuilder(bits.length());
        int altered = 0;
        for (int i = 0; i < bits.length(); i++) {
            char c = bits.charAt(i);
            if (random.nextDouble() < probability) {
                c = (c == '0') ? '1' : '0';
                altered++;
            }
            sb.append(c);
        }
        return new Result(sb.toString(), altered);
    }

    public record Result(String noisyBits, int alteredBits) {
    }
}
