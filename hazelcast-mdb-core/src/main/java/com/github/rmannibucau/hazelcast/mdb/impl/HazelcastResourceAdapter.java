package com.github.rmannibucau.hazelcast.mdb.impl;

import com.github.rmannibucau.hazelcast.mdb.api.HazelcastMessageListener;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.Connector;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

@Connector(vendorName = "rmannibucau", version = "0.1")
public class HazelcastResourceAdapter implements ResourceAdapter {
    private static final String DEFAULT_INSTANCE = "hazelcast";
    private final Collection<HazelcastInstance> handledInstances = new CopyOnWriteArrayList<HazelcastInstance>();

    @Override
    public void start(final BootstrapContext ctx) throws ResourceAdapterInternalException {
        //no-op
    }

    @Override
    public synchronized void stop() {
        for (final HazelcastInstance i : handledInstances) {
            i.getLifecycleService().shutdown();
        }
        handledInstances.clear();
    }

    @Override
    public void endpointActivation(final MessageEndpointFactory endpointFactory, final ActivationSpec spec) throws ResourceException {
        final HazelcastActivationSpec hspec = HazelcastActivationSpec.class.cast(spec);
        if (hspec.getInstance() == null || hspec.getTarget() == null) {
            throw new ResourceException("instance and target should be specified");
        }


        final HazelcastInstance instance = findHazelcastInstance(hspec.getInstance());
        if ("topic".equals(hspec.getTargetType())) {
            final HazelcastMessageListener<Object> endpoint = HazelcastMessageListener.class.cast(endpointFactory.createEndpoint(null));
            final MessageListener<Object> listener = new MessageListener<Object>() {
                @Override
                public void onMessage(final Message<Object> message) {
                    endpoint.onMessage(message.getMessageObject());
                }
            };
            hspec.setListener(listener);
            instance.getTopic(hspec.getTarget()).addMessageListener(listener);
        }
    }

    @Override
    public void endpointDeactivation(final MessageEndpointFactory endpointFactory, final ActivationSpec spec) {
        final HazelcastActivationSpec hspec = HazelcastActivationSpec.class.cast(spec);
        final HazelcastInstance instance = findHazelcastInstance(hspec.getInstance());
        if ("topic".equals(hspec.getTargetType())) {
            instance.getTopic(hspec.getTarget()).removeMessageListener(hspec.getListener());
        }
    }

    @Override
    public XAResource[] getXAResources(final ActivationSpec[] specs) throws ResourceException {
        return new XAResource[0];
    }

    private synchronized HazelcastInstance findHazelcastInstance(final String name) {
        final String instanceName;
        if (name == null) {
            instanceName = DEFAULT_INSTANCE;
        } else {
            instanceName = name;
        }

        HazelcastInstance instance = Hazelcast.getHazelcastInstanceByName(instanceName);
        if (instance != null) {
            return instance;
        }
        instance = Hazelcast.newHazelcastInstance(new XmlConfigBuilder().build().setInstanceName(instanceName));
        handledInstances.add(instance);
        return instance;
    }
}
