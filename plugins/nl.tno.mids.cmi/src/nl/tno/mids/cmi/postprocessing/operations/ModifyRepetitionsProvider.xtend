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
import java.util.Arrays
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode
import nl.tno.mids.cmi.postprocessing.PostProcessingOperationProvider

class ModifyRepetitionsProvider extends PostProcessingOperationProvider<ModifyRepetitions, ModifyRepetitionsOptions> {
    override getOperationReadableName() {
        return "Modify repetitions"
    }

    override getOperationDescription() {
        return "Change the repetition count of detected repetitions and/or represent them differently."
    }

    override getOperationClass() {
        return ModifyRepetitions
    }

    override getOperationOptionsClass() {
        return ModifyRepetitionsOptions
    }

    override getOperation(ModifyRepetitionsOptions options) {
        return new ModifyRepetitions(options)
    }

    override getOptions(String args) {
        val options = new ModifyRepetitionsOptions()
        val argList = Arrays.asList(args.split(","))
        Preconditions.checkArgument(argList.size() == 5, "Invalid arguments for ModifyRepetitions operation: " + args)
        options.mode = MrrToCifMode.valueOf(normalizeEnumValue(argList.get(0)))
        options.lowerThreshold = Integer.valueOf(argList.get(1).trim())
        options.upperThreshold = Integer.valueOf(argList.get(2).trim())
        options.makeInfinite = Boolean.valueOf(argList.get(3).trim())
        options.maxRepeats = Integer.valueOf(argList.get(4).trim())
        return options
    }

    override writeOptions(ModifyRepetitionsOptions options) {
        return displayEnumValue(options.mode.toString) + "," + options.lowerThreshold + "," + options.upperThreshold +
            "," + options.makeInfinite + "," + options.maxRepeats
    }

    override supportsFilteredComponentsAsInput() {
        return true
    }

}
