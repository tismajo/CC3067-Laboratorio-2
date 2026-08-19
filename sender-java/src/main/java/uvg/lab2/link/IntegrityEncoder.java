package uvg.lab2.link;

/**
 * Contrato de la capa de enlace: agrega redundancia a una cadena de bits.
 */
public interface IntegrityEncoder {

    /**
     * @param dataBits bits originales (múltiplo de 8)
     * @return bits con redundancia agregada
     */
    String encode(String dataBits);

    /**
     * Nombre del algoritmo tal como se envía en el campo "algorithm" del protocolo.
     */
    String algorithmName();
}
