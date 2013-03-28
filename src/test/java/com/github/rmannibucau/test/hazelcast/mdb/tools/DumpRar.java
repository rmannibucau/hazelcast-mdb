package com.github.rmannibucau.test.hazelcast.mdb.tools;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStarted;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import java.io.File;

public class DumpRar {
    private final File file = new File("target/tmp/hazelcast-mdb.rar");

    public void dump(final @Observes ManagerStarted before) {
        if (file.exists() && !file.delete()) {
            throw new IllegalStateException("Can't delete " + file.getPath());
        }

        final File parentFile = file.getParentFile();
        if (!parentFile.exists() && !parentFile.mkdirs()) {
            throw new IllegalStateException("Can't create " + file.getParent() + " dir");
        }

        // dump the rar to let openejb deploy it through startup Deployment
        ShrinkWrap.create(GenericArchive.class, "hazelcast-mdb.rar")
                .add(new ArchiveAsset(
                        ShrinkWrap.create(JavaArchive.class)
                                .addPackages(true, "com.github.rmannibucau.hazelcast.mdb"), ZipExporter.class),
                        "hazelcast-mdb.jar")
                //.add(new FileAsset(new File("src/main/rar/META-INF/ra.xml")), "META-INF/ra.xml")
                .as(ZipExporter.class).exportTo(file, true);

    }
}
