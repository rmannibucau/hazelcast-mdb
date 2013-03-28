package com.github.rmannibucau.test.hazelcast.mdb.tools;

import org.jboss.arquillian.core.spi.LoadableExtension;

public class DumpRarExtension implements LoadableExtension {
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.observer(DumpRar.class);
    }
}
