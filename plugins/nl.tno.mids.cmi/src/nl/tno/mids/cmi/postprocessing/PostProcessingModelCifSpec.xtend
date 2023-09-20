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

import com.google.common.base.Preconditions
import nl.tno.mids.automatalib.extensions.cif.CifToAutomataLib
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus
import org.eclipse.escet.cif.metamodel.cif.Specification

class PostProcessingModelCifSpec extends PostProcessingModel {
    val Specification specification

    new(Specification specification, String name, PostProcessingStatus status) {
        super(name, status)
        this.specification = specification
    }

    override getCifSpec() {
        return specification
    }

    override getCompactDfa() {
        Preconditions.checkState(status.dataIsNotPresent)

        val dfas = CifToAutomataLib.cifSpecificationToCompactDfas(specification, false)
        Preconditions.checkState(dfas.size == 1)
        return dfas.entrySet.iterator.next.value
    }

    override recategorizeAsNoData() {
        Preconditions.checkArgument(status.dataIsPresent)
        return new PostProcessingModelCifSpec(specification, name, new PostProcessingStatus(false, status.tauIsPresent))
    }

    override recategorizeAsNoTau() {
        Preconditions.checkArgument(status.tauIsPresent)
        return new PostProcessingModelCifSpec(specification, name,
            new PostProcessingStatus(status.dataIsPresent, false))
    }
}
