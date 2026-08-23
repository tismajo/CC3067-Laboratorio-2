// Package link implementa la capa de enlace del receptor: corrección
// Hamming(12,8) y detección CRC-32/IEEE, ambas manuales.
package link

import "fmt"

var dataPositions = [8]int{3, 5, 6, 7, 9, 10, 11, 12}
var parityPositions = [4]int{1, 2, 4, 8}

// Result es el resultado común de un intento de decodificación/verificación.
type Result struct {
	DataBits       string
	ErrorDetected  bool
	ErrorCorrected bool
	CorrectedBits  int
	Valid          bool
	ErrorMessage   string
}

// HammingDecode procesa el payload en bloques de 12 bits. Cada bloque corrige
// como máximo un bit erróneo; no garantiza detectar errores múltiples dentro
// del mismo bloque (limitación conocida de Hamming(12,8) sin paridad global).
func HammingDecode(payload string) Result {
	if len(payload)%12 != 0 {
		return Result{ErrorMessage: "longitud de payload no es múltiplo de 12", ErrorDetected: true}
	}

	var out []byte
	correctedTotal := 0
	anyError := false

	for offset := 0; offset < len(payload); offset += 12 {
		block := payload[offset : offset+12]
		bits := make([]int, 13) // 1..12 usados
		for i := 0; i < 12; i++ {
			bits[i+1] = int(block[i] - '0')
		}

		syndrome := 0
		for _, p := range parityPositions {
			c := 0
			for i := 1; i <= 12; i++ {
				if i&p != 0 {
					c ^= bits[i]
				}
			}
			syndrome += c * p
		}

		if syndrome != 0 {
			anyError = true
			if syndrome > 12 {
				return Result{
					ErrorDetected: true,
					Valid:         false,
					ErrorMessage:  fmt.Sprintf("síndrome Hamming inválido (%d) en bloque offset %d", syndrome, offset/12),
				}
			}
			bits[syndrome] ^= 1
			correctedTotal++
		}

		for _, p := range dataPositions {
			out = append(out, byte('0'+bits[p]))
		}
	}

	return Result{
		DataBits:       string(out),
		ErrorDetected:  anyError,
		ErrorCorrected: correctedTotal > 0,
		CorrectedBits:  correctedTotal,
		Valid:          true,
	}
}

const crc32Poly = 0xEDB88320

// CRC32Compute calcula el CRC-32/IEEE (ISO-HDLC) de una cadena de bits
// (múltiplo de 8), de forma manual, bit a bit.
func CRC32Compute(dataBits string) uint32 {
	crc := uint32(0xFFFFFFFF)
	for i := 0; i < len(dataBits); i += 8 {
		b := bitsToByte(dataBits[i : i+8])
		crc ^= uint32(b)
		for k := 0; k < 8; k++ {
			if crc&1 != 0 {
				crc = (crc >> 1) ^ crc32Poly
			} else {
				crc = crc >> 1
			}
		}
	}
	return crc ^ 0xFFFFFFFF
}

// CRC32Verify separa datos y CRC del payload y verifica su integridad.
// CRC-32 únicamente detecta errores; nunca corrige.
func CRC32Verify(payload string, originalBitLength int) Result {
	dataBits := payload[:originalBitLength]
	crcBits := payload[originalBitLength:]

	expected := CRC32Compute(dataBits)
	received := bitsToUint32(crcBits)

	if expected != received {
		return Result{
			DataBits:      dataBits,
			ErrorDetected: true,
			Valid:         false,
			ErrorMessage:  "CRC mismatch",
		}
	}
	return Result{
		DataBits: dataBits,
		Valid:    true,
	}
}

func bitsToByte(bits string) byte {
	var v byte
	for i := 0; i < 8; i++ {
		v <<= 1
		if bits[i] == '1' {
			v |= 1
		}
	}
	return v
}

func bitsToUint32(bits string) uint32 {
	var v uint32
	for i := 0; i < len(bits); i++ {
		v <<= 1
		if bits[i] == '1' {
			v |= 1
		}
	}
	return v
}
