// Package presentation convierte cadenas de bits recuperadas en texto ASCII.
package presentation

import "fmt"

// BitsToASCII convierte grupos de 8 bits (MSB-first) a texto ASCII,
// validando que cada byte esté en el rango 0x00-0x7F.
func BitsToASCII(bits string) (string, error) {
	if len(bits)%8 != 0 {
		return "", fmt.Errorf("la longitud de bits (%d) no es múltiplo de 8", len(bits))
	}
	out := make([]byte, 0, len(bits)/8)
	for i := 0; i < len(bits); i += 8 {
		var v byte
		for j := 0; j < 8; j++ {
			c := bits[i+j]
			if c != '0' && c != '1' {
				return "", fmt.Errorf("carácter no binario en posición %d", i+j)
			}
			v <<= 1
			if c == '1' {
				v |= 1
			}
		}
		if v > 0x7F {
			return "", fmt.Errorf("byte fuera de rango ASCII: 0x%02X", v)
		}
		out = append(out, v)
	}
	return string(out), nil
}
