/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing

import java.nio.file.Path
import java.util.LinkedHashMap
import java.util.List
import java.util.Map
import java.util.Map.Entry
import java.util.Set
import java.util.regex.Pattern
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.escet.cif.metamodel.cif.Specification

class PostProcessing {
    private new() {
        // Static class.
    }

    /**
     * Perform post-processing.
     * 
     * @param componentsMap Mapping from component name (absolute name of the CIF automaton) to CIF specification for
     *      that component. May be modified in-place, but should not be used after this call. Use the return value of
     *      this method instead.
     * @param relativeResolvePath The absolute path of the input file. During post-processing, all relative paths are
     *      to be resolved against the directory that contains this file.
     * @param operationsOptions The options for each of the post-processing operations to perform.
     * @param monitor The progress monitor.
     * @return Post-processing result, as mapping from component name (absolute name of the CIF automaton) to CIF
     *      specification for that component.
     */
    static def Map<String, Specification> postProcess(Map<String, Specification> componentsMap,
        Path relativeResolvePath, List<PostProcessingOperationOptions> operationsOptions, IProgressMonitor monitor) {

        val subMonitor = SubMonitor.convert(monitor, operationsOptions.size + 1)

        // Prepare.
        subMonitor.split(1)
        // If there are no post-processing operations configured, no further work is needed.
        if (operationsOptions.empty) {
            return componentsMap
        }
        val modelsToProcess = new LinkedHashMap<String, PostProcessingModel>(componentsMap.size)
        for (Entry<String, Specification> componentEntry : componentsMap.entrySet) {
            val name = componentEntry.key
            val spec = componentEntry.value
            val inputModel = new PostProcessingModelCifSpec(spec, name, new PostProcessingStatus(false, false))
            modelsToProcess.put(name, inputModel)
        }

        // Apply operations.
        for (operationOptions : operationsOptions) {
            subMonitor.taskName = "Performing post-processing operation: " + operationOptions.toString

            // Validate options.
            try {
                operationOptions.validate
            } catch (IllegalStateException e) {
                throw new RuntimeException("Options validation failed: " + operationOptions, e)
            }

            // Select components to which to apply the operation.
            val PostProcessingOperationProvider<?, ?> provider = operationOptions.provider
            var Set<String> selectedComponents
            // If filtering is supported, apply selected filter. Otherwise, all models are always selected.
            if (provider.supportsFilteredComponentsAsInput) {
                selectedComponents = switch (operationOptions.filterMode) {
                    case NONE: {
                        modelsToProcess.keySet
                    }
                    case INCLUSION: {
                        val filterPattern = Pattern.compile(operationOptions.filterPattern)
                        modelsToProcess.keySet.filter(k|filterPattern.matcher(k).matches).toSet
                    }
                    case EXCLUSION: {
                        val filterPattern = Pattern.compile(operationOptions.filterPattern)
                        modelsToProcess.keySet.filter(k|!filterPattern.matcher(k).matches).toSet
                    }
                }
            } else {
                selectedComponents = modelsToProcess.keySet
            }

            // Apply operation. Performs in-place modifications to 'filteredPostProcessMap'.
            val PostProcessingOperation<?> operation = provider.getOperationFromRawOptions(operationOptions)
            operation.applyOperation(modelsToProcess, selectedComponents, relativeResolvePath, subMonitor.split(1))
        }

        // Put result of post-processing in the original specification, modifying it in-place.
        val processedModels = new LinkedHashMap<String, Specification>(modelsToProcess.size)
        for (Entry<String, PostProcessingModel> entry : modelsToProcess.entrySet) {
            val name = entry.key
            val model = entry.value
            val spec = model.getCifSpec()
            processedModels.put(name, spec)
        }
        return processedModels
    }
}
