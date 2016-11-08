package com.joelguilarte.simple.grpc.client;

import com.google.common.base.Stopwatch;
import com.joelguilarte.simple.grpc.home.catalog.Home;
import com.joelguilarte.simple.grpc.home.catalog.HomeCatalogGrpc;
import com.joelguilarte.simple.grpc.home.catalog.Point;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.util.RoundRobinLoadBalancerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by joel on 10/31/16.
 */
public class HomeServiceClient {

    private final ManagedChannel channel;
    private final HomeCatalogGrpc.HomeCatalogBlockingStub blockingStub;

    public HomeServiceClient(String host, int port) {

        channel = NettyChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance()) //Improved Performance dramatically
                .build();

        blockingStub = HomeCatalogGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {

        if (channel != null) {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public void runBenchmark(ExecutorService executor, Point request) {

        for (int x = 0; x < 100; x++) {
            final int index = x;

            Future<Home> task = executor.submit(() -> {
                Home response;

                try {
                    Stopwatch stopwatch = Stopwatch.createUnstarted();

                    stopwatch.start();
                    response = blockingStub.getHome(request);
                    stopwatch.stop();

                    System.out.println("Finished " + index + " RPC call in: "
                            + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");
                } catch (StatusRuntimeException sre) {
                    System.err.println("RPC Failed: " + sre.getStatus());
                    return null;
                }

                return response;
            });
        }
    }

    public static void main(String[] args) throws InterruptedException {

        HomeServiceClient client = new HomeServiceClient("localhost", 50051);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        Point request = Point.newBuilder().setLatitude(20).setLongitude(-80).build();

        Stopwatch benchmarkTime = Stopwatch.createStarted();
        try {
            client.runBenchmark(executor, request);
        } finally {
            executor.shutdown();
            executor.awaitTermination(2, TimeUnit.SECONDS);
            benchmarkTime.stop();
            System.out.println("Finished Benchmark " + benchmarkTime.elapsed(TimeUnit.MILLISECONDS) + "ms");

            client.shutdown();
        }
    }
}