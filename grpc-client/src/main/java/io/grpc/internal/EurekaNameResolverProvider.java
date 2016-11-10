package io.grpc.internal;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;

import javax.annotation.Nullable;
import java.net.URI;

/**
 * Created by joel on 11/9/16.
 */
public class EurekaNameResolverProvider extends NameResolverProvider {

    private static final String EUREKA = "eureka";

    private EurekaInstanceConfig eurekaInstanceConfig;

    public EurekaNameResolverProvider(EurekaInstanceConfig eurekaInstanceConfig) {

        this.eurekaInstanceConfig = eurekaInstanceConfig;
    }

    @Override
    protected boolean isAvailable() {

        return true;
    }

    @Override
    protected int priority() {

        return 5;
    }

    @Nullable
    @Override
    public NameResolver newNameResolver(URI targetUri, Attributes params) {

        return new EurekaNameResolver(eurekaInstanceConfig, targetUri);
    }

    @Override
    public String getDefaultScheme() {

        return EUREKA;
    }
}