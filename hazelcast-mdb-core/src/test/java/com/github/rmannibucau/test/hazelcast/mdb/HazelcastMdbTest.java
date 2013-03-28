package com.github.rmannibucau.test.hazelcast.mdb;

import com.github.rmannibucau.hazelcast.mdb.api.HazelcastMessageListener;
import com.hazelcast.core.Hazelcast;
import org.junit.Test;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.embeddable.EJBContainer;
import javax.naming.NamingException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class HazelcastMdbTest {
    @Test
    public void run() throws NamingException, InterruptedException {
        final EJBContainer container = EJBContainer.createEJBContainer();
        container.getContext().bind("inject", this);

        Hazelcast.getHazelcastInstanceByName("bar").getTopic("foo").publish("hi");
        HazelcastMDB.latch.await(10, TimeUnit.SECONDS);
        assertEquals("hi", HazelcastMDB.lastMessage);

        container.close();
    }

    @MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "target", propertyValue = "foo"),
        @ActivationConfigProperty(propertyName = "targetType", propertyValue = "topic"),
        @ActivationConfigProperty(propertyName = "instance", propertyValue = "bar")
    })
    public static class HazelcastMDB implements HazelcastMessageListener {
        public static CountDownLatch latch = new CountDownLatch(1);
        public static Object lastMessage = null;

        @Override
        public void onMessage(final Object o) {
            lastMessage = o;
            latch.countDown();
        }
    }
}
