package uvg.lab2;

import org.junit.jupiter.api.Test;
import uvg.lab2.link.CRC32Encoder;
import uvg.lab2.link.HammingEncoder;
import uvg.lab2.noise.NoiseSimulator;
import uvg.lab2.presentation.BinaryEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlgorithmsTest {

    @Test
    void asciiToBitsProducesEightBitsMsbFirst() {
        assertEquals("01000001", BinaryEncoder.textToBits("A"));
        assertEquals("0100000101100010", BinaryEncoder.textToBits("Ab"));
    }

    @Test
    void bitsToTextRoundTrip() {
        String bits = BinaryEncoder.textToBits("Hola");
        assertEquals("Hola", BinaryEncoder.bitsToText(bits));
    }

    @Test
    void hammingVectorForLetterA() {
        HammingEncoder encoder = new HammingEncoder();
        String encoded = encoder.encode("01000001");
        assertEquals("100010010001", encoded);
    }

    @Test
    void hammingEncodesMultipleBlocks() {
        HammingEncoder encoder = new HammingEncoder();
        String bits = BinaryEncoder.textToBits("AB");
        String encoded = encoder.encode(bits);
        assertEquals(24, encoded.length());
        assertEquals("100010010001", encoded.substring(0, 12));
    }

    @Test
    void crc32OfCheckString() {
        String bits = BinaryEncoder.textToBits("123456789");
        long crc = CRC32Encoder.compute(bits);
        assertEquals(0xCBF43926L, crc);
    }

    @Test
    void crc32OfLetterA() {
        String bits = BinaryEncoder.textToBits("A");
        long crc = CRC32Encoder.compute(bits);
        assertEquals(0xD3D99E8BL, crc);
    }

    @Test
    void crc32EncodeAppendsThirtyTwoBits() {
        CRC32Encoder encoder = new CRC32Encoder();
        String bits = BinaryEncoder.textToBits("A");
        String encoded = encoder.encode(bits);
        assertEquals(8 + 32, encoded.length());
        assertEquals("11010011110110011001111010001011", encoded.substring(8));
    }

    @Test
    void noiseWithZeroProbabilityLeavesBitsUnchanged() {
        NoiseSimulator sim = new NoiseSimulator(42L);
        String bits = "010101010101";
        NoiseSimulator.Result result = sim.apply(bits, 0.0);
        assertEquals(bits, result.noisyBits());
        assertEquals(0, result.alteredBits());
    }

    @Test
    void noiseWithProbabilityOneFlipsAllBits() {
        NoiseSimulator sim = new NoiseSimulator(42L);
        String bits = "0000111100001111";
        NoiseSimulator.Result result = sim.apply(bits, 1.0);
        assertEquals(bits.length(), result.alteredBits());
        for (int i = 0; i < bits.length(); i++) {
            assertTrue(bits.charAt(i) != result.noisyBits().charAt(i));
        }
    }

    @Test
    void noiseIsReproducibleWithSameSeed() {
        String bits = BinaryEncoder.textToBits("Hola mundo");
        NoiseSimulator sim1 = new NoiseSimulator(1234L);
        NoiseSimulator sim2 = new NoiseSimulator(1234L);
        NoiseSimulator.Result r1 = sim1.apply(bits, 0.3);
        NoiseSimulator.Result r2 = sim2.apply(bits, 0.3);
        assertEquals(r1.noisyBits(), r2.noisyBits());
        assertEquals(r1.alteredBits(), r2.alteredBits());
    }
}
