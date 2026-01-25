package uk.selfemploy.plugin.runtime;

import uk.selfemploy.plugin.api.Plugin;
import uk.selfemploy.plugin.api.PluginContext;
import uk.selfemploy.plugin.api.PluginDescriptor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A test plugin implementation for unit testing.
 */
public class TestPlugin implements Plugin {

    private final String id;
    private final String name;
    private final String version;
    private final String minAppVersion;

    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final AtomicBoolean unloaded = new AtomicBoolean(false);
    private final AtomicInteger loadCount = new AtomicInteger(0);
    private final AtomicInteger unloadCount = new AtomicInteger(0);

    private PluginContext context;
    private RuntimeException loadException;
    private RuntimeException unloadException;

    public TestPlugin() {
        this("uk.selfemploy.plugin.test", "Test Plugin", "1.0.0", "0.1.0");
    }

    public TestPlugin(String id, String name, String version, String minAppVersion) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.minAppVersion = minAppVersion;
    }

    @Override
    public PluginDescriptor getDescriptor() {
        return new PluginDescriptor(id, name, version, "A test plugin", "Test Author", minAppVersion);
    }

    @Override
    public void onLoad(PluginContext context) {
        if (loadException != null) {
            throw loadException;
        }
        this.context = context;
        this.loaded.set(true);
        this.loadCount.incrementAndGet();
    }

    @Override
    public void onUnload() {
        if (unloadException != null) {
            throw unloadException;
        }
        this.unloaded.set(true);
        this.unloadCount.incrementAndGet();
    }

    // Test helpers

    public boolean isLoaded() {
        return loaded.get();
    }

    public boolean isUnloaded() {
        return unloaded.get();
    }

    public int getLoadCount() {
        return loadCount.get();
    }

    public int getUnloadCount() {
        return unloadCount.get();
    }

    public PluginContext getContext() {
        return context;
    }

    public void setLoadException(RuntimeException e) {
        this.loadException = e;
    }

    public void setUnloadException(RuntimeException e) {
        this.unloadException = e;
    }

    public void reset() {
        loaded.set(false);
        unloaded.set(false);
        loadCount.set(0);
        unloadCount.set(0);
        context = null;
        loadException = null;
        unloadException = null;
    }
}
