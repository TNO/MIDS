/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.pps.extensions.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.lsat.common.emf.ecore.resource.Persistor;
import org.eclipse.lsat.common.emf.ecore.resource.PersistorFactory;

import com.google.common.base.Preconditions;

import nl.esi.pps.architecture.ArchitectureModel;
import nl.esi.pps.tmsc.FullScopeTMSC;
import nl.esi.pps.tmsc.ScopedTMSC;
import nl.esi.pps.tmsc.TMSC;
import nl.tno.mids.pps.extensions.cmi.CmiPreparer;
import nl.tno.mids.pps.extensions.cmi.CmiPreparers;

/** Utilities for loading and saving {@link TMSC TMSCs} and their corresponding architectures. */
public class TmscFileHelper {
    private TmscFileHelper() {
    }

    /**
     * Reads a {@link FullScopeTMSC full-scope TMSC} and corresponding architecture from {@code sourceLocation}.
     * 
     * @param sourcePath The path from which the {@link TMSC} and architecture are to be read.
     * @return A {@link Pair} containing the {@link TMSC} and the corresponding architecture that have been read.
     * @throws IOException Thrown in case reading the {@link TMSC} and architecture fails.
     */
    public static Pair<FullScopeTMSC, ArchitectureModel> loadTMSC(Path sourcePath) throws IOException {
        Persistor<EObject> persistor = new PersistorFactory().getPersistor();
        List<EObject> fileContent = persistor.loadAll(URI.createFileURI(sourcePath.toString()));

        Optional<FullScopeTMSC> tmsc = fileContent.stream().filter(obj -> obj instanceof FullScopeTMSC)
                .map(obj -> (FullScopeTMSC)obj).findFirst();
        Optional<ArchitectureModel> architecture = fileContent.stream().filter(obj -> obj instanceof ArchitectureModel)
                .map(obj -> (ArchitectureModel)obj).findFirst();

        Preconditions.checkArgument(tmsc.isPresent(), "Expected a full scope TMSC to be present.");
        Preconditions.checkArgument(architecture.isPresent(), "Expected architecture information to be present.");

        return Pair.pair(tmsc.get(), architecture.get());
    }

    /**
     * Reads a {@link FullScopeTMSC full-scope TMSC} and corresponding architecture from {@code sourceLocation}, and
     * prepares it for CMI by applying a suitable {@link CmiPreparer CMI preparer}.
     * 
     * @param tmscPath The path from which the {@link TMSC} and architecture are to be read.
     * @param warnings The warnings produced during the operation.
     * @return A {@link Pair} containing the loaded and prepared {@link TMSC} and the corresponding architecture that
     *     have been read.
     * @throws IOException Thrown in case reading the {@link TMSC} and architecture fails.
     */
    public static Pair<ScopedTMSC, ArchitectureModel> loadAndPrepareTMSC(Path tmscPath, List<String> warnings)
            throws IOException
    {
        // Load the TMSC and corresponding architecture.
        Pair<FullScopeTMSC, ArchitectureModel> tmscAndArchitecture = loadTMSC(tmscPath);
        FullScopeTMSC fullTmsc = tmscAndArchitecture.left;
        ArchitectureModel architecture = tmscAndArchitecture.right;

        // Determine the CMI preparer for the loaded TMSC, and prepare the TMSC using this preparer.
        CmiPreparer cmiPreparer = CmiPreparers.findFor(fullTmsc);
        ScopedTMSC tmsc = cmiPreparer.prepare(fullTmsc, "CMI", warnings, tmscPath);

        // Return the loaded and prepared TMSC, together with the architecture it uses.
        return Pair.pair(tmsc, architecture);
    }

    /**
     * Saves the underlying {@link FullScopeTMSC full-scope TMSC} of {@code tmsc} to the specified
     * {@code targetLocation}, together with the {@code architecture} that is used by {@code tmsc}.
     * 
     * @param tmsc The {@link TMSC} whose underlying {@link FullScopeTMSC full-scope TMSC} is to be saved.
     * @param architecture The architecture that is used by {@code tmsc}, to be saved.
     * @param targetPath The path to which the specified {@code tmsc} and {@code architecture} are to be written.
     * @throws IOException Thrown in case saving the {@code tmsc} and {@code architecture} fails.
     */
    public static void saveTMSC(TMSC tmsc, ArchitectureModel architecture, Path targetPath) throws IOException {
        Persistor<EObject> persistor = new PersistorFactory().getPersistor();
        persistor.save(URI.createFileURI(targetPath.toString()), Arrays.asList(tmsc.getFullScope(), architecture));
    }
}
