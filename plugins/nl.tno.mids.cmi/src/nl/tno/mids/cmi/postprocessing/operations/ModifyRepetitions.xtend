/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import com.google.common.base.Preconditions
import java.nio.file.Path
import java.util.Map
import java.util.Set
import java.util.function.Predicate
import nl.tno.mids.cif.extensions.mrr.MrrModifyUtils
import nl.tno.mids.cif.extensions.mrr.cif.CifMrrLetter
import nl.tno.mids.cif.extensions.mrr.cif.CifToMrr
import nl.tno.mids.cif.extensions.mrr.cif.CifToMrrConfig
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCif
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode
import nl.tno.mids.cif.extensions.mrr.data.MRR
import nl.tno.mids.cif.extensions.mrr.data.RepetitionMRR
import nl.tno.mids.cmi.postprocessing.PostProcessingModel
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.escet.cif.metamodel.cif.Specification
import org.eclipse.xtend.lib.annotations.Accessors

/** Modify repetitions. Uses {@link MRR Minimal Repetition Representations (MRRs)}. */
@Accessors
class ModifyRepetitions extends PostProcessingOperation<ModifyRepetitionsOptions> {
    override getPreconditionSubset() { return new PostProcessingPreconditionSubset(false, false); }

    override getResultSubset() {
        val useData = options.mode == MrrToCifMode.DATA
        return new PostProcessingResultSubset(useData, useData);
    }

    override applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
        Path relativeResolvePath, IProgressMonitor monitor) {

        val subMonitor = SubMonitor.convert(monitor, selectedComponents.size)
        for (component : selectedComponents) {
            val model = models.get(component)
            preconditionSubset.ensureSubset(model)

            val cifSpec = model.cifSpec
            modifyRepetitions(cifSpec, options, subMonitor.split(1))
            models.put(component, new PostProcessingModelCifSpec(cifSpec, component, getResultStatus(model.status)))
        }
    }

    private static def modifyRepetitions(Specification specification, ModifyRepetitionsOptions options,
        IProgressMonitor monitor) {

        val subMonitor = SubMonitor.convert(monitor, 100);

        // Compute MRRs.
        subMonitor.subTask("Computing repetitions")
        val config = new CifToMrrConfig(1, 1)
        val mrrWithWords = CifToMrr.cifToMrr(specification, config, subMonitor.split(97));

        // Modify MRRs.
        subMonitor.subTask("Applying repetitions modifications")
        subMonitor.split(1);
        Preconditions.checkArgument(!(options.makeInfinite && options.maxRepeats > 0))
        Preconditions.checkArgument(!(options.upperThreshold > 0 && options.lowerThreshold > options.upperThreshold))

        val Predicate<RepetitionMRR<CifMrrLetter>> tresholdFilter = [ r |
            (options.lowerThreshold == 0 || r.getCount() >= options.lowerThreshold) &&
                (options.upperThreshold == 0 || r.getCount() <= options.upperThreshold)
        ];

        for (mrrWithWord : mrrWithWords) {
            if (options.makeInfinite) {
                MrrModifyUtils.mrrSetInfiniteRepeat(mrrWithWord.mrr, tresholdFilter)
            } else if (options.maxRepeats > 0) {
                MrrModifyUtils.mrrRestrictMaxRepeat(mrrWithWord.mrr, options.maxRepeats, tresholdFilter)
            }
        }

        // Apply MRRs.
        subMonitor.split(2);
        for (mrrWithWord : mrrWithWords) {
            MrrToCif.mrrToCif(mrrWithWord, options.mode)
        }
    }

}
