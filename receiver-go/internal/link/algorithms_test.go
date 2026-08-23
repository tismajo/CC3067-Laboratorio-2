package link

import "testing"

func TestHammingDecodeVectorA(t *testing.T) {
	result := HammingDecode("100010010001")
	if !result.Valid {
		t.Fatalf("se esperaba trama válida: %s", result.ErrorMessage)
	}
	if result.DataBits != "01000001" {
		t.Fatalf("dataBits = %s, se esperaba 01000001", result.DataBits)
	}
	if result.ErrorCorrected || result.CorrectedBits != 0 {
		t.Fatalf("no debería haber corrección sin ruido")
	}
}

func TestHammingDecodeSingleBitFlipAllPositions(t *testing.T) {
	base := []byte("100010010001")
	for pos := 0; pos < 12; pos++ {
		flipped := append([]byte(nil), base...)
		if flipped[pos] == '0' {
			flipped[pos] = '1'
		} else {
			flipped[pos] = '0'
		}
		result := HammingDecode(string(flipped))
		if !result.Valid {
			t.Fatalf("posición %d: se esperaba trama válida", pos+1)
		}
		if result.DataBits != "01000001" {
			t.Fatalf("posición %d: dataBits = %s, se esperaba 01000001", pos+1, result.DataBits)
		}
		if !result.ErrorCorrected || result.CorrectedBits != 1 {
			t.Fatalf("posición %d: se esperaba una corrección", pos+1)
		}
	}
}

func TestHammingDecodeMultipleBlocks(t *testing.T) {
	// "A" + "A" = dos bloques idénticos de 12 bits
	payload := "100010010001100010010001"
	result := HammingDecode(payload)
	if !result.Valid {
		t.Fatalf("se esperaba trama válida: %s", result.ErrorMessage)
	}
	if result.DataBits != "0100000101000001" {
		t.Fatalf("dataBits = %s", result.DataBits)
	}
}

func TestCRC32ComputeCheckString(t *testing.T) {
	bits := asciiToBits("123456789")
	crc := CRC32Compute(bits)
	if crc != 0xCBF43926 {
		t.Fatalf("CRC = 0x%08X, se esperaba 0xCBF43926", crc)
	}
}

func TestCRC32VerifyLetterA(t *testing.T) {
	dataBits := asciiToBits("A")
	crcBits := "11010011110110011001111010001011"
	result := CRC32Verify(dataBits+crcBits, len(dataBits))
	if !result.Valid {
		t.Fatalf("se esperaba CRC válido: %s", result.ErrorMessage)
	}
	if result.DataBits != dataBits {
		t.Fatalf("dataBits incorrectos")
	}
}

func TestCRC32VerifyDetectsDataBitFlip(t *testing.T) {
	dataBits := asciiToBits("A")
	crcBits := "11010011110110011001111010001011"
	corrupted := flipBit(dataBits, 0) + crcBits
	result := CRC32Verify(corrupted, len(dataBits))
	if result.Valid {
		t.Fatalf("se esperaba detección de error en los datos")
	}
	if result.ErrorMessage != "CRC mismatch" {
		t.Fatalf("mensaje inesperado: %s", result.ErrorMessage)
	}
}

func TestCRC32VerifyDetectsCrcBitFlip(t *testing.T) {
	dataBits := asciiToBits("A")
	crcBits := "11010011110110011001111010001011"
	corrupted := dataBits + flipBit(crcBits, 0)
	result := CRC32Verify(corrupted, len(dataBits))
	if result.Valid {
		t.Fatalf("se esperaba detección de error en el CRC")
	}
}

func asciiToBits(s string) string {
	out := make([]byte, 0, len(s)*8)
	for _, ch := range []byte(s) {
		for bit := 7; bit >= 0; bit-- {
			if (ch>>uint(bit))&1 == 1 {
				out = append(out, '1')
			} else {
				out = append(out, '0')
			}
		}
	}
	return string(out)
}

func flipBit(bits string, pos int) string {
	b := []byte(bits)
	if b[pos] == '0' {
		b[pos] = '1'
	} else {
		b[pos] = '0'
	}
	return string(b)
}
