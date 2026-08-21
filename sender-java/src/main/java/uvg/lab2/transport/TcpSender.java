package uvg.lab2.transport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Transporte TCP del emisor: envía una línea JSON y espera la línea de
 * respuesta, aplicando timeouts de conexión, lectura y escritura.
 */
public final class TcpSender {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int IO_TIMEOUT_MS = 10000;

    private final String host;
    private final int port;

    public TcpSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String sendAndReceive(String jsonLine) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(IO_TIMEOUT_MS);

            try (PrintWriter out = new PrintWriter(
                    new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                 BufferedReader in = new BufferedReader(
                         new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                out.print(jsonLine);
                out.print('\n');
                out.flush();

                String response = in.readLine();
                if (response == null) {
                    throw new IOException("El receptor cerró la conexión sin responder");
                }
                return response;
            } catch (SocketTimeoutException e) {
                throw new IOException("Timeout esperando respuesta del receptor", e);
            }
        }
    }
}
