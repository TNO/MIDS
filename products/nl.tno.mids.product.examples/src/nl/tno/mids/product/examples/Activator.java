
package nl.tno.mids.product.examples;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
    /** The plug-in id. */
    public static final String PLUGIN_ID = "nl.tno.mids.product.examples";

    /** The shared instance. */
    private static Activator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     *
     * @return The shared instance.
     */
    public static Activator getDefault() {
        return plugin;
    }
}
