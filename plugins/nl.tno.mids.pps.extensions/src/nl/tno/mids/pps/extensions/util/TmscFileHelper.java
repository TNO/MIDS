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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.common.java.Pair;
import org.eclipse.lsat.common.emf.ecore.resource.Persistor;
import org.eclipse.lsat.common.emf.ecore.resource.PersistorFactory;

import com.google.common.base.Preconditions;

import nl.esi.pps.tmsc.FullScopeTMSC;
import nl.esi.pps.tmsc.ScopedTMSC;
import nl.esi.pps.tmsc.TMSC;
import nl.tno.mids.pps.extensions.cmi.CmiPreparer;
import nl.tno.mids.pps.extensions.cmi.CmiPreparers;

/** Utilities for loading and saving {@link TMSC TMSCs}. */
public class TmscFileHelper {
    private TmscFileHelper() {
    }

    /**
     * Reads a {@link FullScopeTMSC full-scope TMSC} from {@code sourceLocation}.
     * 
     * @param sourcePath The path from which the TMSC is to be read.
     * @return The TMSC that has been read.
     * @throws IOException Thrown in case reading the TMSC fails.
     */
    public static FullScopeTMSC loadTMSC(Path sourcePath) throws IOException {
        Persistor<EObject> persistor = new PersistorFactory().getPersistor();
        List<EObject> fileContent = persistor.loadAll(URI.createFileURI(sourcePath.toString()));

        Optional<FullScopeTMSC> tmsc = fileContent.stream().filter(obj -> obj instanceof FullScopeTMSC)
                .map(obj -> (FullScopeTMSC)obj).findFirst();

        Preconditions.checkArgument(tmsc.isPresent(), "Expected a full scope TMSC to be present.");
        Preconditions.checkArgument(!tmsc.get().getArchitectures().isEmpty(),
                "Expected architecture information to be present.");

        return tmsc.get();
    }

    /**
     * Reads a {@link FullScopeTMSC full-scope TMSC} from {@code sourceLocation}, and
     * prepares it for CMI by applying a suitable {@link CmiPreparer CMI preparer}.
     * 
     * @param tmscPath The path from which the TMSC is to be read.
     * @param warnings The warnings produced during the operation.
     * @return The loaded and prepared {@link TMSC} that has been read.
     * @throws IOException Thrown in case reading the TMSC fails.
     */
    public static ScopedTMSC loadAndPrepareTMSC(Path tmscPath, List<String> warnings)
            throws IOException
    {
        // Load the TMSC.
        FullScopeTMSC fullTmsc = loadTMSC(tmscPath);

        // Determine the CMI preparer for the loaded TMSC, and prepare the TMSC using this preparer.
        CmiPreparer cmiPreparer = CmiPreparers.findFor(fullTmsc);
        ScopedTMSC tmsc = cmiPreparer.prepare(fullTmsc, "CMI", warnings, tmscPath);

        // Return the loaded and prepared TMSC.
        return tmsc;
    }

    /**
     * Saves the underlying {@link FullScopeTMSC full-scope TMSC} of {@code tmsc} to the specified
     * {@code targetLocation}.
     * 
     * @param tmsc The {@link TMSC} whose underlying {@link FullScopeTMSC full-scope TMSC} is to be saved.
     * @param targetPath The path to which the specified TMSC is to be written.
     * @throws IOException Thrown in case saving the TMSC fails.
     */
    public static void saveTMSC(TMSC tmsc, Path targetPath) throws IOException {
        Persistor<EObject> persistor = new PersistorFactory().getPersistor();
        ArrayList<EObject> contents = new ArrayList<>(tmsc.getFullScope().getArchitectures().size() + 1);
        contents.add(tmsc.getFullScope());
        contents.addAll(tmsc.getFullScope().getArchitectures());
        persistor.save(URI.createFileURI(targetPath.toString()), contents);
    }
}
