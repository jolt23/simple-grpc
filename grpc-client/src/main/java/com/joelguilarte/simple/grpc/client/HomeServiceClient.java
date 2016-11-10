package com.joelguilarte.simple.grpc.client;

import com.google.common.base.Stopwatch;
import com.joelguilarte.simple.grpc.home.catalog.Home;
import com.joelguilarte.simple.grpc.home.catalog.HomeCatalogGrpc;
import com.joelguilarte.simple.grpc.home.catalog.Point;
import io.grpc.*;
import io.grpc.internal.ManagedChannelImpl;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.util.RoundRobinLoadBalancerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by joel on 10/31/16.
 */
public class HomeServiceClient {

    private final ManagedChannel channel;
    private final HomeCatalogGrpc.HomeCatalogBlockingStub blockingStub;

    public HomeServiceClient(String host, int port) {

        channel = NettyChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
                .build();

        blockingStub = HomeCatalogGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {

        if (channel != null) {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public void runBenchmark(Point request) {

        try {
            Stopwatch stopwatch = Stopwatch.createUnstarted();

            stopwatch.start();
            blockingStub.getHome(request);
            stopwatch.stop();

            System.out.println("Finished RPC call in: "
                    + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");
        } catch (StatusRuntimeException sre) {
            System.err.println("RPC Failed: " + sre.getStatus());
        }
    }

    public void channelWarmUp() {

        ClientCall<Point, Home> call = channel.newCall(HomeCatalogGrpc.METHOD_GET_HOME, CallOptions.DEFAULT);
        ClientCalls.blockingUnaryCall(call, null);
    }

    public static void main(String[] args) throws InterruptedException {

        HomeServiceClient client = new HomeServiceClient("localhost", 50051);
        Point request = Point.newBuilder().setLatitude(20).setLongitude(-80).build();

        client.channelWarmUp();

        Stopwatch benchmarkTime = Stopwatch.createStarted();
        try {
            client.runBenchmark(request);
        } finally {
            benchmarkTime.stop();
            System.out.println("Finished Benchmark " + benchmarkTime.elapsed(TimeUnit.MILLISECONDS) + "ms");

            client.shutdown();
        }
    }
}