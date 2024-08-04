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

import net.automatalib.automata.fsa.impl.compact.CompactDFA
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus
import org.eclipse.escet.cif.metamodel.cif.Specification

abstract class PostProcessingModel {
    public val String name

    public val PostProcessingStatus status

    new(String name, PostProcessingStatus status) {
        this.name = name
        this.status = status
    }

    abstract def Specification getCifSpec()

    abstract def CompactDFA<String> getCompactDfa()

    /** Re-categorizes this model as having no data. Must only be invoked if currently may have data. */
    abstract def PostProcessingModel recategorizeAsNoData()

    /** Re-categorizes this model as having no tau. Must only be invoked if currently may have tau events. */
    abstract def PostProcessingModel recategorizeAsNoTau()
  
}
