/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.common;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

/** MIDS executable provider. */
public interface MidsExecutableProvider {
    /**
     * Get the executable name or path for an executable.
     * 
     * @param executableName The name of the executable.
     * @return The executable name or path. May return {@code null} if this provider does not provide a path for the
     *     given executable.
     */
    public String getExecutable(String executableName);

    /**
     * Get the executable name or path for an executable. Uses providers provided by the extension point to get a
     * name/path, and if none of them provides it, uses the executable name as is.
     * 
     * @param executableName The name of the executable.
     * @return The executable name or path.
     */
    public static String getExecutablePath(String executableName) {
        // Use extension registry to find registered providers.
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        String extensionPointId = "nl.tno.mids.common.executables";
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor(extensionPointId);

        // Get providers.
        List<MidsExecutableProvider> providers = new ArrayList<>();
        for (IConfigurationElement extension: extensions) {
            // Check for providers extension.
            if (!"provider".equals(extension.getName())) {
                continue;
            }

            // Get OSGi bundle.
            String pluginName = extension.getAttribute("plugin");
            Bundle bundle = Platform.getBundle(pluginName);
            if (bundle == null) {
                throw new RuntimeException("MIDS executables provider plugin not found: " + pluginName);
            }

            // Check bundle state.
            int state = bundle.getState();
            boolean stateOk = state == Bundle.RESOLVED || state == Bundle.STARTING || state == Bundle.ACTIVE;
            if (!stateOk) {
                throw new RuntimeException(
                        "MIDS executables provider plugin in wrong state: " + pluginName + " (state=" + state + ")");
            }

            // Get class loader from bundle.
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring == null) {
                throw new RuntimeException("MIDS executables provider plugin has no bundle wiring: " + pluginName);
            }
            ClassLoader classLoader = bundleWiring.getClassLoader();
            if (classLoader == null) {
                throw new RuntimeException("MIDS executables provider plugin has no class loader: " + pluginName);
            }

            // Get class.
            String className = extension.getAttribute("class");
            Class<?> cls;
            try {
                cls = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("MIDS executables provider plugin is missing the required class: "
                        + pluginName + " (class=" + className + ")");
            }

            // Get provider.
            MidsExecutableProvider provider;
            try {
                provider = (MidsExecutableProvider)cls.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("MIDS executables provider plugin has class that could not be instantiated: "
                        + pluginName + " (class=" + className + ")");
            }

            // Add provider.
            providers.add(provider);
        }

        // Get executable path from provider.
        for (MidsExecutableProvider provider: providers) {
            String path = provider.getExecutable(executableName);
            if (path != null) {
                return path;
            }
        }

        // By default, if no provider provides a path, use the executable name itself.
        return executableName;
    }
}
