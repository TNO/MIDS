/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

public class PostProcessingOperationProviders {
    private PostProcessingOperationProviders() {
    }

    public static final List<PostProcessingOperationProvider<?, ?>> PROVIDERS = Collections
            .unmodifiableList(getPostProcessingOperationProviders());

    private static List<PostProcessingOperationProvider<?, ?>> getPostProcessingOperationProviders() {
        // Use extension registry to find registered providers.
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        String extensionPointId = "nl.tno.mids.cmi.postprocessing";
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor(extensionPointId);

        // Get providers.
        List<PostProcessingOperationProvider<?, ?>> providers = new ArrayList<>();
        for (IConfigurationElement extension: extensions) {
            // Check for providers extension.
            if (!"provider".equals(extension.getName())) {
                continue;
            }

            // Get OSGi bundle.
            String pluginName = extension.getAttribute("plugin");
            Bundle bundle = Platform.getBundle(pluginName);
            if (bundle == null) {
                throw new RuntimeException("CMI post-processing operation provider plugin not found: " + pluginName);
            }

            // Check bundle state.
            int state = bundle.getState();
            boolean stateOk = state == Bundle.RESOLVED || state == Bundle.STARTING || state == Bundle.ACTIVE;
            if (!stateOk) {
                throw new RuntimeException("CMI post-processing operation provider plugin in wrong state: " + pluginName
                        + " (state=" + state + ")");
            }

            // Get class loader from bundle.
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring == null) {
                throw new RuntimeException(
                        "CMI post-processing operation provider plugin has no bundle wiring: " + pluginName);
            }
            ClassLoader classLoader = bundleWiring.getClassLoader();
            if (classLoader == null) {
                throw new RuntimeException(
                        "CMI post-processing operation provider plugin has no class loader: " + pluginName);
            }

            // Get class.
            String className = extension.getAttribute("class");
            Class<?> cls;
            try {
                cls = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(
                        "CMI post-processing operation provider plugin is missing the required class: " + pluginName
                                + " (class=" + className + ")");
            }

            // Get provider.
            PostProcessingOperationProvider<?, ?> provider;
            try {
                provider = (PostProcessingOperationProvider<?, ?>)cls.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(
                        "CMI post-processing operation provider plugin has class that could not be instantiated: "
                                + pluginName + " (class=" + className + ")");
            }

            // Add provider.
            providers.add(provider);
        }

        return providers;
    }

    public static PostProcessingOperationProvider<?, ?> getPostProcessingOperationProvider(String name) {
        for (PostProcessingOperationProvider<?, ?> provider: PROVIDERS) {
            if (provider.getOperationClass().getSimpleName().equals(name)) {
                return provider;
            }
        }
        return null;
    }

    public static PostProcessingOperationProvider<?, ?>
            getPostProcessingOperationProvider(PostProcessingOperationOptions options)
    {
        for (PostProcessingOperationProvider<?, ?> provider: PROVIDERS) {
            if (provider.getOperationOptionsClass().isInstance(options)) {
                return provider;
            }
        }
        throw new RuntimeException("No suitable provider found: " + options);
    }
}
