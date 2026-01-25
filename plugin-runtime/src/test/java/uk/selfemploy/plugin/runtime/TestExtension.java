package uk.selfemploy.plugin.runtime;

import uk.selfemploy.plugin.extension.ExtensionPoint;

/**
 * A simple test extension point for unit testing.
 */
public interface TestExtension extends ExtensionPoint {

    String getName();

    void execute();
}
