/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cif.extensions;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.escet.cif.io.emf.CifResourceFactory;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.lsat.common.emf.common.util.URIHelper;
import org.eclipse.lsat.common.emf.ecore.resource.Persistor;
import org.eclipse.lsat.common.emf.ecore.resource.PersistorFactory;

/**
 * This file mostly exists because XTEND somehow has a hard time dealing with the (generic) types of {@link Persistor}
 * class and the {@link CifResourceFactory} is not known.
 */
public class FileExtensions {
    /**
     * Write CIF {@link Specification} to disk. For best performance, use {@link #saveCIF(Specification, Path)} instead.
     * 
     * @param cif {@link Specification} to write.
     * @param cifFile File to write specification to.
     * @throws IOException when an I/O error occurs.
     */
    public static void saveCIF(Specification cif, IFile cifFile) throws IOException {
        Persistor<Specification> persistorCIF = new PersistorFactory().getPersistor(Specification.class);
        persistorCIF.save(URI.createPlatformResourceURI(cifFile.getFullPath().toString(), true), cif);
    }

    /**
     * Write CIF {@link Specification} to disk.
     * 
     * @param cif {@link Specification} to write.
     * @param cifPath {@link Path} to write specification to.
     * @throws IOException when an I/O error occurs.
     */
    public static void saveCIF(Specification cif, Path cifPath) throws IOException {
        Persistor<Specification> persistorCIF = new PersistorFactory().getPersistor(Specification.class);
        persistorCIF.save(URI.createFileURI(cifPath.toString()), cif);
    }

    /**
     * Read CIF {@link Specification} from disk.
     * 
     * @param cifFile File to read.
     * @return {@link Specification} read from file.
     * @throws IOException when an I/O error occurs.
     */
    public static Specification loadCIF(IFile cifFile) throws IOException {
        Persistor<Specification> persistorCIF = new PersistorFactory().getPersistor(Specification.class);
        return persistorCIF.loadOne(URIHelper.asURI(cifFile));
    }

    /**
     * @param cifPath {@link Path} to file to read.
     * @return {@link Specification} read from file.
     * @throws IOException when an I/O error occurs.
     */
    public static Specification loadCIF(Path cifPath) throws IOException {
        Persistor<Specification> persistorCIF = new PersistorFactory().getPersistor(Specification.class);
        return persistorCIF.loadOne(URI.createFileURI(cifPath.toString()));
    }
}
