// Package protocol define las estructuras del protocolo compartido con el
// emisor Java y su validación de campos.
package protocol

import (
	"bytes"
	"encoding/json"
	"fmt"
)

const (
	AlgorithmHamming = "HAMMING_12_8"
	AlgorithmCRC32   = "CRC32_IEEE"
)

// Frame es la trama recibida del emisor.
type Frame struct {
	Version           int     `json:"version"`
	MessageID         string  `json:"messageId"`
	Algorithm         string  `json:"algorithm"`
	OriginalBitLength int     `json:"originalBitLength"`
	EncodedBitLength  int     `json:"encodedBitLength"`
	ErrorProbability  float64 `json:"errorProbability"`
	Payload           string  `json:"payload"`
}

// Response es la respuesta enviada de vuelta al emisor.
type Response struct {
	Version        int    `json:"version"`
	MessageID      string `json:"messageId"`
	Algorithm      string `json:"algorithm"`
	Valid          bool   `json:"valid"`
	ErrorDetected  bool   `json:"errorDetected"`
	ErrorCorrected bool   `json:"errorCorrected"`
	CorrectedBits  int    `json:"correctedBits"`
	Message        string `json:"message,omitempty"`
	Error          string `json:"error,omitempty"`
}

// ParseFrame decodifica una línea JSON en un Frame.
func ParseFrame(line []byte) (Frame, error) {
	var f Frame
	dec := json.NewDecoder(bytes.NewReader(line))
	dec.DisallowUnknownFields()
	if err := dec.Decode(&f); err != nil {
		return Frame{}, fmt.Errorf("JSON inválido: %w", err)
	}
	return f, nil
}

// Validate revisa todos los campos según las reglas del protocolo.
func (f Frame) Validate() error {
	if f.Version != 1 {
		return fmt.Errorf("version debe ser 1")
	}
	if f.MessageID == "" {
		return fmt.Errorf("messageId no puede estar vacío")
	}
	if f.Algorithm != AlgorithmHamming && f.Algorithm != AlgorithmCRC32 {
		return fmt.Errorf("algorithm desconocido: %s", f.Algorithm)
	}
	if f.OriginalBitLength <= 0 || f.OriginalBitLength%8 != 0 {
		return fmt.Errorf("originalBitLength debe ser positivo y múltiplo de 8")
	}
	if f.ErrorProbability < 0 || f.ErrorProbability > 1 {
		return fmt.Errorf("errorProbability debe estar entre 0 y 1")
	}
	if len(f.Payload) != f.EncodedBitLength {
		return fmt.Errorf("encodedBitLength no coincide con la longitud del payload")
	}
	for _, c := range f.Payload {
		if c != '0' && c != '1' {
			return fmt.Errorf("payload contiene caracteres distintos de 0/1")
		}
	}
	switch f.Algorithm {
	case AlgorithmHamming:
		expected := (f.OriginalBitLength / 8) * 12
		if f.EncodedBitLength != expected {
			return fmt.Errorf("encodedBitLength inválido para Hamming: esperado %d", expected)
		}
	case AlgorithmCRC32:
		expected := f.OriginalBitLength + 32
		if f.EncodedBitLength != expected {
			return fmt.Errorf("encodedBitLength inválido para CRC-32: esperado %d", expected)
		}
	}
	return nil
}
