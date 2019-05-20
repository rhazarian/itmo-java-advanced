package ru.ifmo.rain.alekperov.udp;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloClientImpl implements HelloClient {

    private static final int TIMEOUT = 1000;

    private boolean validate(final String request, final String response) {
        return "Hello, ".concat(request).equals(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (final UnknownHostException ex) {
            System.err.println("Unknown host at hello client:");
            System.err.println(ex.getMessage());
            return;
        }
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int id = 0; id < threads; ++id) {
            final int thread = id;
            executor.submit(() -> {
                try (final DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(TIMEOUT);
                    final var bufferSize = socket.getReceiveBufferSize();
                    for (int i = 0; i < requests; ++i) {
                        final String request = prefix + thread + "_" + i;
                        final var bytes = request.getBytes(StandardCharsets.UTF_8);
                        final var packet = new DatagramPacket(bytes, bytes.length, address, port);
                        while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                            try {
                                socket.send(packet);
                                System.out.println("Request:");
                                System.out.println(request);
                                final var response = Util.getMessage(Util.receivePacket(socket, bufferSize));
                                if (validate(request, response)) {
                                    System.out.println("Response:");
                                    System.out.println(response);
                                    break;
                                }
                            } catch (final IOException ex) {
                                System.err.println("I/O error at hello client:");
                                System.err.println(ex.getMessage());
                            }
                        }
                    }
                } catch (final SocketException ex) {
                    System.err.println("Socket error at hello client:");
                    System.err.println(ex.getMessage());
                }
            });
        }
        executor.shutdown();
        try {
            executor.awaitTermination(threads * requests * TIMEOUT * 100, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException ignored) { }
    }
}
