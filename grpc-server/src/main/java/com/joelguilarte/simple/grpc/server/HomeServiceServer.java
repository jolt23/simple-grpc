package com.joelguilarte.simple.grpc.server;

import com.joelguilarte.simple.grpc.service.HomeCatalogServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

/**
 * Created by joel on 10/31/16.
 */
public class HomeServiceServer {

    private int port = 50051;
    private Server server;

    private void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new HomeCatalogServiceImpl())
                .build()
                .start();

        System.out.println("Started HomeServiceServer, listening on port: " + 50051);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                HomeServiceServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {

        if (server != null) {
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {

        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        final HomeServiceServer server = new HomeServiceServer();
        server.start();
        server.blockUntilShutdown();
    }
}