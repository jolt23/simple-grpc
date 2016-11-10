package com.joelguilarte.simple.grpc.server;

import com.google.inject.AbstractModule;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.guice.EurekaModule;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;

/**
 * Created by joel on 11/8/16.
 */
public class HomeServiceGovernator {

    static class HomeServiceCatalogModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(HomeServiceServer.class).asEagerSingleton();
        }
    }

    private static LifecycleInjector init() throws Exception {
        System.out.println("Creating injector for HomeServiceCatalog.");

        LifecycleInjector injector = InjectorBuilder
                .fromModules(new EurekaModule(), new HomeServiceCatalogModule())
                .overrideWith(new AbstractModule() {
                    @Override
                    protected void configure() {
                        DynamicPropertyFactory configInstance = DynamicPropertyFactory.getInstance();
                        bind(DynamicPropertyFactory.class).toInstance(configInstance);
                        bind(EurekaInstanceConfig.class).to(MyDataCenterInstanceConfig.class);
                    }
                })
                .createInjector();

        System.out.println("Finished creating HomeServiceCatalog injector.");
        return injector;
    }

    public static void main(String[] args) {

        LifecycleInjector injector = null;

        try {
            injector = init();
            injector.awaitTermination();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (injector != null) {
                injector.shutdown();
            }
        }
    }
}
