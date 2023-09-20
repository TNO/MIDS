/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////
package nl.tno.mids.cmi.postprocessing.operations

import com.google.common.base.Preconditions
import java.util.Locale
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationOptions
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider
import org.eclipse.xtend.lib.annotations.Accessors

@Accessors
class ModifyRepetitionsOptions extends PostProcessingOperationOptions {

    /** Whether to make repetitions infinite. */
    public boolean makeInfinite

    /**
     * Restrict number of repetitions, if number of repetitions at least this value. {@code 0} for no lower bound.
     * Must not be enabled if {@link #makeInfinite} is enabled.
     * Must not be enabled if {@link #maxRepeats} is disabled.
     */
    public int lowerThreshold

    /**
     * Restrict number of repetitions, if number of repetitions at most this value. {@code 0} for no upper bound.
     * Must not be enabled if {@link #makeInfinite} is enabled.
     * Must not be enabled if {@link #maxRepeats} is disabled.
     */
    public int upperThreshold

    /**
     * Restrict number of repetitions to this value. {@code 0} to disable.
     * Must not be enabled if {@link #makeInfinite} is enabled.
     */
    public int maxRepeats

    /** How to modify the repetitions. */
    public MrrToCifMode mode = MrrToCifMode.PLAIN

    override validate() throws IllegalStateException {
        Preconditions.checkState(!(upperThreshold > 0 && lowerThreshold > upperThreshold))
        Preconditions.checkState(!(maxRepeats > 0 && makeInfinite))
    }

    override toString() {
        val builder = new StringBuilder()
        builder.append(PostProcessingOperationProvider.getOperationFormalName(ModifyRepetitions))
        builder.append(" ")
        builder.append(mode.toString.toLowerCase(Locale.US))
        if (makeInfinite) {
            builder.append(" makeInfinite")
        } else if (maxRepeats > 0) {
            builder.append(" maxRepeats ")
            builder.append(maxRepeats)
            if (lowerThreshold > 0) {
                builder.append(" lowerThreshold ")
                builder.append(lowerThreshold)
            }
            if (upperThreshold > 0) {
                builder.append(" upperThreshold ")
                builder.append(upperThreshold)
            }
        }
        builder.append(" ")
        builder.append(super.toString)
        return builder.toString
    }
}
