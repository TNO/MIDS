/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing

import java.nio.file.Path
import java.util.Map
import java.util.Set
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus
import org.eclipse.core.runtime.IProgressMonitor

abstract class PostProcessingOperation<U extends PostProcessingOperationOptions> implements Cloneable {
    protected U options

    new(U options) {
        this.options = options
    }

    abstract def PostProcessingPreconditionSubset getPreconditionSubset()

    abstract def PostProcessingResultSubset getResultSubset()

    /**
     * Perform a post-processing operation.
     * 
     * @param models Mapping from component name (absolute name of the CIF automaton) to input model for the component.
     *      This mapping should be modified in-place.
     * @param selectedComponents Names of the components from {@code models} to which to apply the operation.
     * @param relativeResolvePath The absolute path of the input file. During post-processing, all relative paths are
     *      to be resolved against the directory that contains this file.
     * @param monitor The progress monitor.
     */
    abstract def void applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor)

    /**
     * The resultStatus is the result of applying the modificationSubset onto the initialStatus.
     */
    def final getResultStatus(PostProcessingStatus status) {
        val ensuredStatus = preconditionSubset.apply(status)
        return resultSubset.apply(ensuredStatus)
    }
}
