package com.github.rmannibucau.hazelcast.mdb.impl;

import com.github.rmannibucau.hazelcast.mdb.api.HazelcastMessageListener;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemListener;
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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@Connector(vendorName = "rmannibucau", version = "0.1", eisType = "Hazelcast MDB Adapter")
public class HazelcastResourceAdapter implements ResourceAdapter {
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

        final int poolSize = hspec.getPoolSize();
        final BlockingQueue<HazelcastMessageListener<Object>> pool = new ArrayBlockingQueue<HazelcastMessageListener<Object>>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.add(HazelcastMessageListener.class.cast(endpointFactory.createEndpoint(null)));
        }
        final HazelcastMessageListener<Object> endpoint = HazelcastMessageListener.class.cast(
                Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                        new Class<?>[] { HazelcastMessageListener.class }, new InvocationHandler() {
                    @Override
                    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
                        final HazelcastMessageListener<Object> listener = pool.take();
                        try {
                            return method.invoke(listener, args);
                        } finally { // we currently don't handle listener invalidation, shouldn't hurt if the listener is not stateful
                            pool.add(listener);
                        }
                    }
                }));

        final HazelcastInstance instance = findHazelcastInstance(hspec.getInstance());
        final String type = hspec.getTargetType();
        if ("topic".equals(type)) {
            final MessageListener<Object> listener = new DelegateMessageListener(endpoint);
            hspec.setListener(listener);
            instance.getTopic(hspec.getTarget()).addMessageListener(listener);
        } else if ("map".equals(type)) {
            final EntryListener<Object, Object> listener = new DelegateEntryListener(endpoint);
            hspec.setListener(listener);
            instance.getMap(hspec.getTarget()).addEntryListener(listener, hspec.getIncludeValue());
        } else if ("queue".equals(type)) {
            final ItemListener<Object> listener = new DelegateItemListener(endpoint);
            hspec.setListener(listener);
            instance.getQueue(hspec.getTarget()).addItemListener(listener, hspec.getIncludeValue());
        } else if ("multi-map".equals(type)) {
            final EntryListener<Object, Object> listener = new DelegateEntryListener(endpoint);
            hspec.setListener(listener);
            instance.getMultiMap(hspec.getTarget()).addEntryListener(listener, hspec.getIncludeValue());
        } else if ("list".equals(type)) {
            final ItemListener<Object> listener = new DelegateItemListener(endpoint);
            hspec.setListener(listener);
            instance.getList(hspec.getTarget()).addItemListener(listener, hspec.getIncludeValue());
        } else if ("set".equals(type)) {
            final ItemListener<Object> listener = new DelegateItemListener(endpoint);
            hspec.setListener(listener);
            instance.getSet(hspec.getTarget()).addItemListener(listener, hspec.getIncludeValue());
        } else {
            throw new IllegalArgumentException("Type " + type + " is not handled");
        }
    }

    @Override
    public void endpointDeactivation(final MessageEndpointFactory endpointFactory, final ActivationSpec spec) {
        final HazelcastActivationSpec hspec = HazelcastActivationSpec.class.cast(spec);
        final HazelcastInstance instance = findHazelcastInstance(hspec.getInstance());
        final String type = hspec.getTargetType();
        if ("topic".equals(type)) {
            instance.getTopic(hspec.getTarget()).removeMessageListener(MessageListener.class.cast(hspec.getListener()));
        } else if ("map".equals(type)) {
            instance.getMap(hspec.getTarget()).removeEntryListener(EntryListener.class.cast(hspec.getListener()));
        } else if ("queue".equals(type)) {
            instance.getQueue(hspec.getTarget()).removeItemListener(ItemListener.class.cast(hspec.getListener()));
        } else if ("multi-map".equals(type)) {
            instance.getMap(hspec.getTarget()).removeEntryListener(EntryListener.class.cast(hspec.getListener()));
        } else if ("list".equals(type)) {
            instance.getList(hspec.getTarget()).removeItemListener(ItemListener.class.cast(hspec.getListener()));
        } else if ("set".equals(type)) {
            instance.getSet(hspec.getTarget()).removeItemListener(ItemListener.class.cast(hspec.getListener()));
        }
    }

    @Override
    public XAResource[] getXAResources(final ActivationSpec[] specs) throws ResourceException {
        return new XAResource[0];
    }

    private synchronized HazelcastInstance findHazelcastInstance(final String name) {
        HazelcastInstance instance = Hazelcast.getHazelcastInstanceByName(name);
        if (instance != null) {
            return instance;
        }
        instance = Hazelcast.newHazelcastInstance(new XmlConfigBuilder().build().setInstanceName(name));
        handledInstances.add(instance);
        return instance;
    }

    private static class DelegateItemListener implements ItemListener<Object> {
        private final HazelcastMessageListener<Object> endpoint;

        public DelegateItemListener(final HazelcastMessageListener<Object> endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public synchronized void itemAdded(final ItemEvent<Object> item) {
            endpoint.onMessage(item);
        }

        @Override
        public synchronized void itemRemoved(final ItemEvent<Object> item) {
            endpoint.onMessage(item);
        }
    }

    private static class DelegateEntryListener implements EntryListener<Object, Object> {
        private final HazelcastMessageListener<Object> endpoint;

        public DelegateEntryListener(final HazelcastMessageListener<Object> endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public synchronized void entryAdded(final EntryEvent<Object, Object> event) {
            endpoint.onMessage(event);
        }

        @Override
        public synchronized void entryRemoved(final EntryEvent<Object, Object> event) {
            endpoint.onMessage(event);
        }

        @Override
        public synchronized void entryUpdated(final EntryEvent<Object, Object> event) {
            endpoint.onMessage(event);
        }

        @Override
        public synchronized void entryEvicted(final EntryEvent<Object, Object> event) {
            endpoint.onMessage(event);
        }
    }

    private static class DelegateMessageListener implements MessageListener<Object> {
        private final HazelcastMessageListener<Object> endpoint;

        public DelegateMessageListener(final HazelcastMessageListener<Object> endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public synchronized void onMessage(final Message<Object> message) {
            endpoint.onMessage(message);
        }
    }
}
