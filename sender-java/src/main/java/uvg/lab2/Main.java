package uvg.lab2;

import uvg.lab2.application.MessageInput;
import uvg.lab2.link.CRC32Encoder;
import uvg.lab2.link.HammingEncoder;
import uvg.lab2.link.IntegrityEncoder;
import uvg.lab2.noise.NoiseSimulator;
import uvg.lab2.presentation.BinaryEncoder;
import uvg.lab2.protocol.Frame;
import uvg.lab2.protocol.Response;
import uvg.lab2.transport.TcpSender;

import java.util.Locale;
import java.util.Scanner;
import java.util.UUID;

public final class Main {

    public static void main(String[] args) {
        String host = "127.0.0.1";
        int port = 8080;
        Long seed = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--seed" -> seed = Long.parseLong(args[++i]);
                default -> {
                    System.err.println("Argumento desconocido: " + args[i]);
                    System.exit(1);
                }
            }
        }

        try (Scanner scanner = new Scanner(System.in)) {
            MessageInput input = new MessageInput(scanner);

            String message = input.readAsciiMessage();
            int algorithmChoice = input.readAlgorithmChoice();
            double probability = input.readErrorProbability();

            IntegrityEncoder encoder = (algorithmChoice == 1) ? new HammingEncoder() : new CRC32Encoder();
            NoiseSimulator noise = (seed != null) ? new NoiseSimulator(seed) : new NoiseSimulator();

            String originalBits = BinaryEncoder.textToBits(message);
            String encodedBits = encoder.encode(originalBits);
            NoiseSimulator.Result noisy = noise.apply(encodedBits, probability);

            int overheadBits = encodedBits.length() - originalBits.length();
            double overheadPercent = (originalBits.length() == 0) ? 0.0
                    : (overheadBits * 100.0) / originalBits.length();

            System.out.println();
            System.out.println("Bits originales:            " + originalBits);
            System.out.println("Trama codificada (sin ruido): " + encodedBits);
            System.out.println("Trama tras ruido:            " + noisy.noisyBits());
            System.out.println("Bits alterados por el ruido: " + noisy.alteredBits());
            System.out.printf(Locale.US, "Overhead: %d bits (%.2f%%)%n", overheadBits, overheadPercent);

            Frame frame = new Frame(
                    UUID.randomUUID().toString(),
                    encoder.algorithmName(),
                    originalBits.length(),
                    encodedBits.length(),
                    probability,
                    noisy.noisyBits()
            );

            System.out.println();
            System.out.println("Enviando trama a " + host + ":" + port + " ...");
            TcpSender sender = new TcpSender(host, port);
            String responseLine = sender.sendAndReceive(frame.toJson());

            Response response = Response.fromJson(responseLine);
            System.out.println();
            System.out.println("Respuesta del receptor:");
            System.out.println("  messageId:      " + response.messageId);
            System.out.println("  algorithm:      " + response.algorithm);
            System.out.println("  valid:          " + response.valid);
            System.out.println("  errorDetected:  " + response.errorDetected);
            System.out.println("  errorCorrected: " + response.errorCorrected);
            System.out.println("  correctedBits:  " + response.correctedBits);
            if (response.valid) {
                System.out.println("  message:        " + response.message);
            } else {
                System.out.println("  error:          " + response.error);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error de validación: " + e.getMessage());
            System.exit(1);
        } catch (java.io.IOException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            System.exit(1);
        }
    }
}
