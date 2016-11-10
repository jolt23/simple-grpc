package com.joelguilarte.simple.grpc.server;

import com.joelguilarte.simple.grpc.service.HomeCatalogServiceImpl;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.EurekaClient;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

/**
 * Created by joel on 10/31/16.
 */
@Singleton
public class HomeServiceServer {

    private Server server;

    private final ApplicationInfoManager applicationInfoManager;
    private final EurekaClient eurekaClient;
    private final DynamicPropertyFactory configInstance;

    @Inject
    public HomeServiceServer(ApplicationInfoManager applicationInfoManager,
                             EurekaClient eurekaClient,
                             DynamicPropertyFactory configInstance) {

        this.applicationInfoManager = applicationInfoManager;
        this.eurekaClient = eurekaClient;
        this.configInstance = configInstance;
    }

    @PostConstruct
    public void start() throws IOException, InterruptedException {

        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.STARTING);

        int port = applicationInfoManager.getInfo().getPort();

        server = NettyServerBuilder.forPort(port)
                .addService(new HomeCatalogServiceImpl())
                .build()
                .start();

        System.out.println("Started HomeServiceServer, listening on port: " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                HomeServiceServer.this.stop();
                System.err.println("*** server shut down");
            }
        });

        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);

        waitForRegistrationWithEureka(eurekaClient);
    }

    public void stop() {

        if (eurekaClient != null) {
            System.out.println("Shutting down EurekaClient.");
            eurekaClient.shutdown();
        }

        if (server != null) {
            System.out.println("Shutting down gRPC Server");
            server.shutdown();
        }
    }

    private void waitForRegistrationWithEureka(EurekaClient eurekaClient) {

        String vipAddress = configInstance.getStringProperty("eureka.vipAddress", "homeservice.local").get();

        InstanceInfo nextServerInfo = null;
        while(nextServerInfo == null) {
            try {
                nextServerInfo = eurekaClient.getNextServerFromEureka(vipAddress, false);
            } catch (Throwable e) {
                System.out.println("Waiting .. verifying service registration with eureka ...");

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
}