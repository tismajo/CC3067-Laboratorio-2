// Comando receiver: arranca el servidor TCP del receptor.
package main

import (
	"context"
	"flag"
	"log"
	"os/signal"
	"strconv"
	"syscall"

	"uvg.lab2/receiver-go/internal/transport"
)

func main() {
	log.SetFlags(0)

	host := flag.String("host", "0.0.0.0", "dirección de escucha")
	port := flag.Int("port", 8080, "puerto de escucha")
	flag.Parse()

	server, err := transport.Listen(*host, strconv.Itoa(*port))
	if err != nil {
		log.Fatalf("no se pudo iniciar el servidor: %v", err)
	}

	ctx, stop := signal.NotifyContext(context.Background(), syscall.SIGINT, syscall.SIGTERM)
	defer stop()

	log.Printf("[LISTENING] address=%s", server.Addr().String())

	go func() {
		<-ctx.Done()
		log.Printf("[SHUTDOWN] señal recibida, cerrando listener")
		_ = server.Close()
	}()

	server.Serve()
	log.Printf("[SHUTDOWN] receptor detenido")
}
