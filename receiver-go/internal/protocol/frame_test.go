package protocol

import "testing"

func validHammingFrame() Frame {
	return Frame{
		Version:           1,
		MessageID:         "test-001",
		Algorithm:         AlgorithmHamming,
		OriginalBitLength: 8,
		EncodedBitLength:  12,
		ErrorProbability:  0.01,
		Payload:           "100010010001",
	}
}

func TestParseFrameInvalidJSON(t *testing.T) {
	_, err := ParseFrame([]byte("{not json"))
	if err == nil {
		t.Fatal("se esperaba error al parsear JSON inválido")
	}
}

func TestValidateOK(t *testing.T) {
	if err := validHammingFrame().Validate(); err != nil {
		t.Fatalf("no se esperaba error: %v", err)
	}
}

func TestValidateWrongVersion(t *testing.T) {
	f := validHammingFrame()
	f.Version = 2
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por version inválida")
	}
}

func TestValidateEmptyMessageID(t *testing.T) {
	f := validHammingFrame()
	f.MessageID = ""
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por messageId vacío")
	}
}

func TestValidateUnknownAlgorithm(t *testing.T) {
	f := validHammingFrame()
	f.Algorithm = "UNKNOWN"
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por algoritmo desconocido")
	}
}

func TestValidateNonBinaryPayload(t *testing.T) {
	f := validHammingFrame()
	f.Payload = "10001001000X"
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por payload no binario")
	}
}

func TestValidateInconsistentLength(t *testing.T) {
	f := validHammingFrame()
	f.EncodedBitLength = 13
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por longitud inconsistente")
	}
}

func TestValidateOriginalBitLengthNotMultipleOfEight(t *testing.T) {
	f := validHammingFrame()
	f.OriginalBitLength = 10
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por originalBitLength no múltiplo de 8")
	}
}

func TestValidateErrorProbabilityOutOfRange(t *testing.T) {
	f := validHammingFrame()
	f.ErrorProbability = 1.5
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por errorProbability fuera de rango")
	}
}

func TestValidateHammingEncodedLengthMismatch(t *testing.T) {
	f := validHammingFrame()
	f.OriginalBitLength = 16
	// EncodedBitLength sigue en 12, debería ser 24
	f.Payload = "100010010001"
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por encodedBitLength inconsistente con Hamming")
	}
}

func TestValidateCRCEncodedLengthMismatch(t *testing.T) {
	f := validHammingFrame()
	f.Algorithm = AlgorithmCRC32
	f.EncodedBitLength = 12 // debería ser 8+32=40
	if err := f.Validate(); err == nil {
		t.Fatal("se esperaba error por encodedBitLength inconsistente con CRC")
	}
}
