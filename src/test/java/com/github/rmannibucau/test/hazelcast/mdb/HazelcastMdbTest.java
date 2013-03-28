package com.github.rmannibucau.test.hazelcast.mdb;

import com.github.rmannibucau.hazelcast.mdb.api.HazelcastMessageListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.impl.DataMessage;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.naming.NamingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(Arquillian.class)
public class HazelcastMdbTest {
    @Deployment
    public static JavaArchive jar() {
        return ShrinkWrap.create(JavaArchive.class, "app.jar")
                    .addClass(HazelcastMDB.class);
    }

    @Test
    public void run() throws NamingException, InterruptedException {
        Hazelcast.getHazelcastInstanceByName("bar").getTopic("foo").publish("hi");
        HazelcastMDB.latch.await(10, TimeUnit.SECONDS);
        assertEquals("hi", HazelcastMDB.lastMessage);
    }

    @MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "target", propertyValue = "foo"),
        @ActivationConfigProperty(propertyName = "targetType", propertyValue = "topic"),
        @ActivationConfigProperty(propertyName = "instance", propertyValue = "bar")
    })
    public static class HazelcastMDB implements HazelcastMessageListener<DataMessage<String>> {
        public static CountDownLatch latch = new CountDownLatch(1);
        public static Object lastMessage = null;

        @Override
        public void onMessage(final DataMessage<String> o) {
            lastMessage = o.getMessageObject();
            latch.countDown();
        }
    }
}
