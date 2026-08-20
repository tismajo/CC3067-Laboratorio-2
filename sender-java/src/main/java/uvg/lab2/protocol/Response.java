package uvg.lab2.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Respuesta del receptor. Se parsea con un pequeño parser JSON de objetos
 * planos (sin anidamiento), suficiente para el esquema fijo del protocolo.
 */
public final class Response {

    public final int version;
    public final String messageId;
    public final String algorithm;
    public final boolean valid;
    public final boolean errorDetected;
    public final boolean errorCorrected;
    public final int correctedBits;
    public final String message; // null si valid == false
    public final String error;   // null si valid == true

    private Response(int version, String messageId, String algorithm, boolean valid,
                      boolean errorDetected, boolean errorCorrected, int correctedBits,
                      String message, String error) {
        this.version = version;
        this.messageId = messageId;
        this.algorithm = algorithm;
        this.valid = valid;
        this.errorDetected = errorDetected;
        this.errorCorrected = errorCorrected;
        this.correctedBits = correctedBits;
        this.message = message;
        this.error = error;
    }

    public static Response fromJson(String json) {
        Map<String, Object> map = parseFlatObject(json);
        return new Response(
                ((Number) map.getOrDefault("version", 0L)).intValue(),
                (String) map.get("messageId"),
                (String) map.get("algorithm"),
                Boolean.TRUE.equals(map.get("valid")),
                Boolean.TRUE.equals(map.get("errorDetected")),
                Boolean.TRUE.equals(map.get("errorCorrected")),
                ((Number) map.getOrDefault("correctedBits", 0L)).intValue(),
                (String) map.get("message"),
                (String) map.get("error")
        );
    }

    private static Map<String, Object> parseFlatObject(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        int i = json.indexOf('{') + 1;
        int end = json.lastIndexOf('}');
        if (end < 0) {
            throw new IllegalArgumentException("JSON inválido: falta '}'");
        }
        while (i < end) {
            char c = json.charAt(i);
            if (c == ' ' || c == ',' || c == '\n' || c == '\r' || c == '\t') {
                i++;
                continue;
            }
            if (c != '"') {
                throw new IllegalArgumentException("JSON inválido cerca de: " + json.substring(i));
            }
            int keyStart = ++i;
            while (json.charAt(i) != '"') {
                i++;
            }
            String key = json.substring(keyStart, i);
            i++; // cierre de comilla de la clave
            while (json.charAt(i) == ' ' || json.charAt(i) == ':') {
                i++;
            }
            Object value;
            char v = json.charAt(i);
            if (v == '"') {
                StringBuilder sb = new StringBuilder();
                i++;
                while (json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\') {
                        i++;
                        sb.append(json.charAt(i));
                    } else {
                        sb.append(json.charAt(i));
                    }
                    i++;
                }
                value = sb.toString();
                i++;
            } else if (v == 't') {
                value = Boolean.TRUE;
                i += 4;
            } else if (v == 'f') {
                value = Boolean.FALSE;
                i += 5;
            } else if (v == 'n') {
                value = null;
                i += 4;
            } else {
                int numStart = i;
                while (i < end && "-+.0123456789eE".indexOf(json.charAt(i)) >= 0) {
                    i++;
                }
                String num = json.substring(numStart, i);
                value = num.contains(".") ? (Object) Double.valueOf(num) : (Object) Long.valueOf(num);
            }
            map.put(key, value);
        }
        return map;
    }
}
