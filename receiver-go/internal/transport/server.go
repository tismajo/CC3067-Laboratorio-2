// Package transport implementa el servidor TCP del receptor: acepta
// conexiones concurrentes, procesa un JSON por línea y responde en JSON.
package transport

import (
	"bufio"
	"encoding/json"
	"log"
	"net"
	"time"

	"uvg.lab2/receiver-go/internal/link"
	"uvg.lab2/receiver-go/internal/presentation"
	"uvg.lab2/receiver-go/internal/protocol"
)

const (
	maxLineBytes = 1 << 20 // 1 MiB por mensaje
	idleTimeout  = 30 * time.Second
)

// Server encapsula el listener TCP y su ciclo de vida.
type Server struct {
	listener net.Listener
}

// Listen crea el listener en host:port.
func Listen(host string, port string) (*Server, error) {
	ln, err := net.Listen("tcp", net.JoinHostPort(host, port))
	if err != nil {
		return nil, err
	}
	return &Server{listener: ln}, nil
}

// Addr retorna la dirección en la que escucha el servidor.
func (s *Server) Addr() net.Addr {
	return s.listener.Addr()
}

// Close cierra el listener, provocando que Serve retorne.
func (s *Server) Close() error {
	return s.listener.Close()
}

// Serve acepta conexiones indefinidamente y despacha una goroutine por cada
// una. Retorna cuando el listener se cierra (apagado ordenado).
func (s *Server) Serve() {
	for {
		conn, err := s.listener.Accept()
		if err != nil {
			log.Printf("[STOPPED] listener cerrado: %v", err)
			return
		}
		go handleConnection(conn)
	}
}

func handleConnection(conn net.Conn) {
	defer conn.Close()

	scanner := bufio.NewScanner(conn)
	scanner.Buffer(make([]byte, 0, 64*1024), maxLineBytes)

	for {
		_ = conn.SetDeadline(time.Now().Add(idleTimeout))
		if !scanner.Scan() {
			if err := scanner.Err(); err != nil {
				log.Printf("[CONN_ERROR] %v", err)
			}
			return
		}
		line := append([]byte(nil), scanner.Bytes()...)
		if len(line) == 0 {
			continue
		}
		response := processLine(line)
		if err := writeResponse(conn, response); err != nil {
			log.Printf("[CONN_ERROR] escritura fallida: %v", err)
			return
		}
	}
}

func writeResponse(conn net.Conn, resp protocol.Response) error {
	data, err := json.Marshal(resp)
	if err != nil {
		return err
	}
	data = append(data, '\n')
	_ = conn.SetWriteDeadline(time.Now().Add(idleTimeout))
	_, err = conn.Write(data)
	return err
}

func processLine(line []byte) protocol.Response {
	frame, err := protocol.ParseFrame(line)
	if err != nil {
		log.Printf("[REJECTED] id=unknown reason=%q", err.Error())
		return protocol.Response{
			Version: 1,
			Valid:   false,
			Error:   err.Error(),
		}
	}

	if err := frame.Validate(); err != nil {
		log.Printf("[REJECTED] id=%s reason=%q", frame.MessageID, err.Error())
		return protocol.Response{
			Version:       1,
			MessageID:     frame.MessageID,
			Algorithm:     frame.Algorithm,
			Valid:         false,
			ErrorDetected: true,
			Error:         err.Error(),
		}
	}

	log.Printf("[RECEIVED] id=%s algorithm=%s encodedBits=%d",
		frame.MessageID, frame.Algorithm, frame.EncodedBitLength)

	var result link.Result
	switch frame.Algorithm {
	case protocol.AlgorithmHamming:
		result = link.HammingDecode(frame.Payload)
	case protocol.AlgorithmCRC32:
		result = link.CRC32Verify(frame.Payload, frame.OriginalBitLength)
	}

	if !result.Valid {
		log.Printf("[REJECTED] id=%s reason=%q", frame.MessageID, result.ErrorMessage)
		return protocol.Response{
			Version:       1,
			MessageID:     frame.MessageID,
			Algorithm:     frame.Algorithm,
			Valid:         false,
			ErrorDetected: result.ErrorDetected,
			Error:         result.ErrorMessage,
		}
	}

	if result.CorrectedBits > 0 {
		log.Printf("[CORRECTED] id=%s correctedBits=%d", frame.MessageID, result.CorrectedBits)
	}

	text, err := presentation.BitsToASCII(result.DataBits)
	if err != nil {
		log.Printf("[REJECTED] id=%s reason=%q", frame.MessageID, err.Error())
		return protocol.Response{
			Version:       1,
			MessageID:     frame.MessageID,
			Algorithm:     frame.Algorithm,
			Valid:         false,
			ErrorDetected: true,
			Error:         err.Error(),
		}
	}

	log.Printf("[MESSAGE] id=%s text=%q", frame.MessageID, text)

	return protocol.Response{
		Version:        1,
		MessageID:      frame.MessageID,
		Algorithm:      frame.Algorithm,
		Valid:          true,
		ErrorDetected:  result.ErrorDetected,
		ErrorCorrected: result.ErrorCorrected,
		CorrectedBits:  result.CorrectedBits,
		Message:        text,
	}
}
