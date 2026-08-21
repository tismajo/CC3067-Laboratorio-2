package uvg.lab2.application;

import java.util.Scanner;

/**
 * Capa de aplicación: interacción por consola con el usuario.
 */
public final class MessageInput {

    private final Scanner scanner;

    public MessageInput(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Pide un mensaje ASCII (0x00-0x7F). Reintenta hasta recibir uno válido. */
    public String readAsciiMessage() {
        while (true) {
            System.out.print("Mensaje a enviar (ASCII): ");
            String line = scanner.nextLine();
            if (line.isEmpty()) {
                System.out.println("El mensaje no puede estar vacío.");
                continue;
            }
            if (isAscii(line)) {
                return line;
            }
            System.out.println("Error: el mensaje contiene caracteres fuera del rango ASCII 0x00-0x7F.");
        }
    }

    private boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    /** 1 = Hamming(12,8), 2 = CRC-32/IEEE. */
    public int readAlgorithmChoice() {
        while (true) {
            System.out.print("Algoritmo [1=Hamming(12,8), 2=CRC-32/IEEE]: ");
            String line = scanner.nextLine().trim();
            if (line.equals("1") || line.equals("2")) {
                return Integer.parseInt(line);
            }
            System.out.println("Opción inválida. Ingrese 1 o 2.");
        }
    }

    public double readErrorProbability() {
        while (true) {
            System.out.print("Probabilidad de error por bit [0.0 - 1.0]: ");
            String line = scanner.nextLine().trim();
            try {
                double p = Double.parseDouble(line);
                if (p >= 0.0 && p <= 1.0) {
                    return p;
                }
                System.out.println("La probabilidad debe estar entre 0 y 1.");
            } catch (NumberFormatException e) {
                System.out.println("Valor numérico inválido.");
            }
        }
    }
}
