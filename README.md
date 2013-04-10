# Goal

Use MDB API to listen Hazelcast messages.

# Basic usage

    @MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "target", propertyValue = "foo"),
        @ActivationConfigProperty(propertyName = "targetType", propertyValue = "topic"),
        @ActivationConfigProperty(propertyName = "instance", propertyValue = "bar"),
        @ActivationConfigProperty(propertyName = "poolSize", propertyValue = "10")
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

# Configuration (as activation properties)


| Name          | Value                                                          |
| ------------- | -------------------------------------------------------------- |
| target        | name of the structure (map, queue...)                          |
| targetType    | type of the structure: topic, map, queue, multi-map, list, set |
| instance      | hazelcast instance name if not the default one (hazelcast)     |
| poolSize      | number of listeners to create                                  |

# HazelcastMessageListener interface

This interface marks a Hazelcast MDB. Its generic type is the type of listened message. here are the type by targetType:

| targetType | Message type                        |
| ---------- | ----------------------------------- |
| topic      | com.hazelcast.core.Message<?>       |
| map        | com.hazelcast.core.EntryEvent<?, ?> |
| queue      | com.hazelcast.core.ItemEvent<?>     |
| multi-map  | com.hazelcast.core.EntryEvent<?, ?> |
| list       | com.hazelcast.core.ItemEvent<?>     |
| set        | com.hazelcast.core.ItemEvent<?>     |
