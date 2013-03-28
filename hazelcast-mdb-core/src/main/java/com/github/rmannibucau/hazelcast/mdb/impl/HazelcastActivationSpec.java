package com.github.rmannibucau.hazelcast.mdb.impl;

import com.github.rmannibucau.hazelcast.mdb.api.HazelcastMessageListener;
import com.hazelcast.core.MessageListener;

import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import java.io.Serializable;

@Activation(messageListeners = HazelcastMessageListener.class)
public class HazelcastActivationSpec implements ActivationSpec, Serializable {
    private String instance;
    private String target;
    private String targetType;

    private ResourceAdapter ra;
    private HazelcastMessageListener endpoint;
    private MessageListener<Object> listener;

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

    public void setListener(final MessageListener<Object> listener) {
        this.listener = listener;
    }

    public MessageListener<Object> getListener() {
        return listener;
    }
}
