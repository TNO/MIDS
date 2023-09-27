/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.pps.extensions.cmi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import nl.esi.pps.tmsc.Dependency;
import nl.esi.pps.tmsc.TMSC;

public class CmiPreparers {
    /**
     * Determines the CMI preparer to use for a TMSC. This preparer is defined to be the first registered preparer that
     * {@link CmiPreparer#appliesTo applies to} the first {@link Dependency} of the TMSC for which such any preparer
     * applies.
     * <p>
     * This method does not check if the preparer applies to all dependencies of the TMSC. Such a check would require
     * scanning all dependencies of the TMSC, which may have an impact on performance especially when the TMSC is big.
     * </p>
     * 
     * @param tmsc The TMSC for which to find the CMI preparer.
     * @return The preparer to use.
     * @throws IllegalArgumentException Thrown in case a CMI preparer could not be determined, for example because the
     *     TMSC contains only dependencies that are not to be considered by CMI.
     */
    public static CmiPreparer findFor(TMSC tmsc) throws IllegalArgumentException {
        List<CmiPreparer> preparers = getRegisteredCmiPreparers();

        Optional<CmiPreparer> preparer = tmsc.getDependencies().stream().flatMap(
                d -> preparers.stream().filter(p -> p.appliesTo(d))).findFirst();

        if (!preparer.isPresent()) {
            throw new IllegalArgumentException("Could not determine the CMI preparer to use for the TMSC.");
        }

        return preparer.get();
    }

    private static List<CmiPreparer> getRegisteredCmiPreparers() {
        // Use extension registry to find registered preparers.
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        String extensionPointId = "nl.tno.mids.pps.extensions.cmi.preparers";
        IConfigurationElement[] extensions = registry.getConfigurationElementsFor(extensionPointId);

        // Get preparers.
        List<CmiPreparer> preparers = new ArrayList<>();
        for (IConfigurationElement extension: extensions) {
            // Check for preparer extension.
            if (!"preparer".equals(extension.getName())) {
                continue;
            }

            // Get OSGi bundle.
            String pluginName = extension.getAttribute("plugin");
            Bundle bundle = Platform.getBundle(pluginName);
            if (bundle == null) {
                throw new RuntimeException("CMI preparer plugin not found: " + pluginName);
            }

            // Check bundle state.
            int state = bundle.getState();
            boolean stateOk = state == Bundle.RESOLVED || state == Bundle.STARTING || state == Bundle.ACTIVE;
            if (!stateOk) {
                throw new RuntimeException(
                        "CMI preparer plugin in wrong state: " + pluginName + " (state=" + state + ")");
            }

            // Get class loader from bundle.
            BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
            if (bundleWiring == null) {
                throw new RuntimeException("CMI preparer plugin has no bundle wiring: " + pluginName);
            }
            ClassLoader classLoader = bundleWiring.getClassLoader();
            if (classLoader == null) {
                throw new RuntimeException("CMI preparer plugin has no class loader: " + pluginName);
            }

            // Get class.
            String className = extension.getAttribute("class");
            Class<?> cls;
            try {
                cls = classLoader.loadClass(className);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("CMI preparer plugin is missing the required class: " + pluginName
                        + " (class=" + className + ")");
            }

            // Get preparer.
            CmiPreparer preparer;
            try {
                preparer = (CmiPreparer)cls.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("CMI preparer plugin has class that could not be instantiated: " + pluginName
                        + " (class=" + className + ")");
            }

            // Add preparer.
            preparers.add(preparer);
        }

        return preparers;
    }
}
