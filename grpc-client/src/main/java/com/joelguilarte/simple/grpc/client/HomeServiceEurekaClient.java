package com.joelguilarte.simple.grpc.client;

import com.google.common.base.Stopwatch;
import com.joelguilarte.simple.grpc.home.catalog.Home;
import com.joelguilarte.simple.grpc.home.catalog.HomeCatalogGrpc;
import com.joelguilarte.simple.grpc.home.catalog.Point;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.appinfo.providers.MyDataCenterInstanceConfigProvider;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.util.RoundRobinLoadBalancerFactory;
import sun.jvm.hotspot.oops.Instance;

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

    private static ApplicationInfoManager applicationInfoManager;
    private static EurekaClient eurekaClient;

    private static synchronized ApplicationInfoManager initializeApplicationInfoManager(EurekaInstanceConfig instanceConfig) {

        if (applicationInfoManager == null) {
            InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
            applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
        }

        return applicationInfoManager;
    }

    private static synchronized EurekaClient initializeEurekaClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig) {

        if (eurekaClient == null) {
            eurekaClient = new DiscoveryClient(applicationInfoManager, clientConfig);
        }

        return eurekaClient;
    }

    public HomeServiceEurekaClient(String host, int port) {

        System.out.println("Creating new Channel for host: " + host + ":" + port );

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

    public void runBenchmark() throws InterruptedException {

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

        ClientCall<Point, Home> call = channel.newCall(HomeCatalogGrpc.METHOD_GET_HOME, CallOptions.DEFAULT);
        ClientCalls.blockingUnaryCall(call, null);
    }

    public static void main(String[] args) throws InterruptedException {

        ApplicationInfoManager applicationInfoManager = initializeApplicationInfoManager(new MyDataCenterInstanceConfig());
        EurekaClient eurekaClient1 = initializeEurekaClient(applicationInfoManager, new DefaultEurekaClientConfig());

        String vipAddress = "homeservice.local";

        InstanceInfo nextServerInfo = null;

        try {
            nextServerInfo = eurekaClient.getNextServerFromEureka(vipAddress, false);
        } catch (Exception e) {
            System.err.println("Cannot get an instance of example service to talk to from eureka");
            System.exit(-1);
        }

        System.out.println("Found an instance of example service to talk to from eureka: "
                + nextServerInfo.getVIPAddress() + ":" + nextServerInfo.getPort());

        HomeServiceEurekaClient client = new HomeServiceEurekaClient(nextServerInfo.getHostName(), nextServerInfo.getPort());

        client.channelWarmUp();
        client.runBenchmark();

        eurekaClient.shutdown();
    }
}