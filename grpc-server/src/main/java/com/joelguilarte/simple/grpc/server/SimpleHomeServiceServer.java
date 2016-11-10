package com.joelguilarte.simple.grpc.server;

import com.joelguilarte.simple.grpc.service.HomeCatalogServiceImpl;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;

/**
 * Created by joel on 11/9/16.
 */
public class SimpleHomeServiceServer {

    private Server server;

    public void start() throws IOException, InterruptedException {

        server = NettyServerBuilder.forPort(50051)
                .addService(new HomeCatalogServiceImpl())
                .build()
                .start();

        System.out.println("Started HomeServiceServer, listening on port: " + 50051);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                SimpleHomeServiceServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {

        if (server != null) {
            System.out.println("Shutting down gRPC Server");
            server.shutdown();
        }
    }

    private void blockUntilShutdown() throws InterruptedException {

        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) {

        final SimpleHomeServiceServer simpleHomeServiceServer = new SimpleHomeServiceServer();

        try {
            simpleHomeServiceServer.start();
            simpleHomeServiceServer.blockUntilShutdown();
        } catch (Throwable e) {
            System.out.println("Error duing SimpleHomeServiceServer initialization.");
            e.printStackTrace();
        }
    }
}