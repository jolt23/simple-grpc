package io.grpc.internal;

import com.google.common.base.Preconditions;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import io.grpc.Attributes;
import io.grpc.NameResolver;
import io.grpc.ResolvedServerInfo;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by joel on 11/9/16.
 */
public class EurekaNameResolver extends NameResolver {

    ApplicationInfoManager applicationInfoManager;

    private EurekaClient eurekaClient;
    private String vipAddress;

    private Listener listener;

    public EurekaNameResolver(EurekaInstanceConfig eurekaInstanceConfig, URI targetUri) {

        InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(eurekaInstanceConfig).get();

        applicationInfoManager = new ApplicationInfoManager(eurekaInstanceConfig, instanceInfo);
        eurekaClient = new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig());
        vipAddress = targetUri.getPath();
    }

    @Override
    public String getServiceAuthority() {

        return vipAddress;
    }

    @Override
    public void start(Listener listener) {

        Preconditions.checkState(this.listener == null, "already started");
        this.listener = Preconditions.checkNotNull(listener, "listener");

        List<InstanceInfo> instances = eurekaClient.getInstancesByVipAddress(vipAddress, false);
        List<ResolvedServerInfo> servers = instances.stream()
                .map(i -> new ResolvedServerInfo(new InetSocketAddress(i.getHostName(), i.getPort()), Attributes.EMPTY))
                .collect(Collectors.toList());

        listener.onUpdate(Collections.singletonList(servers), Attributes.EMPTY);
    }

    @Override
    public void shutdown() {

        eurekaClient.shutdown();
    }
}