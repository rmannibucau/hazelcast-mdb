package com.github.rmannibucau.hazelcast.mdb.impl;

import com.github.rmannibucau.hazelcast.mdb.api.HazelcastMessageListener;

import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import java.io.Serializable;
import java.util.EventListener;

@Activation(messageListeners = HazelcastMessageListener.class)
public class HazelcastActivationSpec implements ActivationSpec, Serializable {
    private String target;
    private String instance = "hazelcast";
    private String targetType = "map";
    private boolean includeValue = false; // is supported
    private int poolSize = 10;

    private ResourceAdapter ra;
    private HazelcastMessageListener endpoint;
    private EventListener listener;

    @Override
    public void validate() throws InvalidPropertyException {
        // no-op
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return ra;
    }

    @Override
    public void setResourceAdapter(final ResourceAdapter ra) throws ResourceException {
        this.ra = ra;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(final String instance) {
        this.instance = instance;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(final String targetType) {
        this.targetType = targetType;
    }

    public boolean getIncludeValue() {
        return includeValue;
    }

    public void setIncludeValue(final boolean includeValue) {
        this.includeValue = includeValue;
    }

    public void setListener(final EventListener listener) {
        this.listener = listener;
    }

    public EventListener getListener() {
        return listener;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
}
