package com.github.rmannibucau.hazelcast.mdb.api;

public interface HazelcastMessageListener<T> {
    void onMessage(T o);
}
