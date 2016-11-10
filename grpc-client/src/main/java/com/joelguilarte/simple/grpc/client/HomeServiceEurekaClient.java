package com.joelguilarte.simple.grpc.client;

import com.google.common.base.Stopwatch;
import com.joelguilarte.simple.grpc.home.catalog.Home;
import com.joelguilarte.simple.grpc.home.catalog.HomeCatalogGrpc;
import com.joelguilarte.simple.grpc.home.catalog.Point;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.EurekaNameResolverProvider;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.util.RoundRobinLoadBalancerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by joel on 10/31/16.
 */
public class HomeServiceEurekaClient {

    private final ManagedChannel channel;
    private final HomeCatalogGrpc.HomeCatalogBlockingStub blockingStub;

    public HomeServiceEurekaClient(EurekaInstanceConfig eurekaInstanceConfig, String target) {

        System.out.println("Creating new Channel for host: " + target);

        channel = NettyChannelBuilder.forTarget(target)
                .usePlaintext(true)
                .nameResolverFactory(new EurekaNameResolverProvider(eurekaInstanceConfig))
                .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance()) //Improved Performance dramatically
                .build();

        blockingStub = HomeCatalogGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {

        if (channel != null) {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public void runBenchmark() throws InterruptedException {

        System.out.println("Started Benchmark");

        ExecutorService executor = Executors.newFixedThreadPool(20);
        Point request = Point.newBuilder().setLatitude(20).setLongitude(-80).build();

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

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
    }

    public void channelWarmUp() {

        System.out.println("Started warmup.");

        ClientCall<Point, Home> call = channel.newCall(HomeCatalogGrpc.METHOD_GET_HOME, CallOptions.DEFAULT);
        ClientCalls.blockingUnaryCall(call, null);
    }

    public static void main(String[] args) throws InterruptedException {

        MyDataCenterInstanceConfig instanceConfig = new MyDataCenterInstanceConfig();
        String vipAddress = "homeservice.local";

        HomeServiceEurekaClient client = new HomeServiceEurekaClient(instanceConfig, vipAddress);

        client.channelWarmUp();
        client.runBenchmark();
    }
}