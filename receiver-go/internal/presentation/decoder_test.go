package presentation

import "testing"

func TestBitsToASCIIRoundTrip(t *testing.T) {
	// "Hola" en bits MSB-first
	bits := "01001000011011110110110001100001"
	text, err := BitsToASCII(bits)
	if err != nil {
		t.Fatalf("no se esperaba error: %v", err)
	}
	if text != "Hola" {
		t.Fatalf("text = %q, se esperaba \"Hola\"", text)
	}
}

func TestBitsToASCIINotMultipleOfEight(t *testing.T) {
	_, err := BitsToASCII("0100000")
	if err == nil {
		t.Fatal("se esperaba error por longitud no múltiplo de 8")
	}
}

func TestBitsToASCIIInvalidByte(t *testing.T) {
	// 0x80 no es ASCII válido (>0x7F)
	_, err := BitsToASCII("10000000")
	if err == nil {
		t.Fatal("se esperaba error por byte fuera de rango ASCII")
	}
}

func TestBitsToASCIINonBinaryChar(t *testing.T) {
	_, err := BitsToASCII("0100000X")
	if err == nil {
		t.Fatal("se esperaba error por carácter no binario")
	}
}
