/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.postprocessing.operations;

import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.common.java.Strings;

import com.google.common.base.Preconditions;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.info.ComponentInfo;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;

/**
 * Renames a component based on a specified old name and new name. This post-processing operation assumes that no
 * filtering has been applied (e.g., component exclusion/inclusion filtering).
 */
public class RenameComponent extends PostProcessingOperation<RenameComponentOptions> {
    /**
     * Construct {@link RenameComponent} operation.
     * 
     * @param options Configuration of operation to construct.
     */
    public RenameComponent(RenameComponentOptions options) {
        super(options);
    }

    @Override
    public PostProcessingPreconditionSubset getPreconditionSubset() {
        return new PostProcessingPreconditionSubset(false, false);
    }

    @Override
    public PostProcessingResultSubset getResultSubset() {
        return new PostProcessingResultSubset(false, false);
    }

    @Override
    public void applyOperation(Map<String, PostProcessingModel> models, Set<String> selectedComponents,
            Path relativeResolvePath, IProgressMonitor monitor)
    {
        monitor.subTask("Renaming component");

        // Since we look at the complete set of models, it must be the case that every component that occurs in
        // 'models' has a behavioral model that is mapped by 'models'.
        for (String component: models.keySet()) {
            PostProcessingModel model = models.get(component);
            getPreconditionSubset().ensureSubset(model);
            for (ComplexComponent complexComponent: CmiBasicComponentQueries.getComponents(model.getCifSpec())) {
                String componentName = CmiGeneralComponentQueries.getComponentName(complexComponent);
                Preconditions.checkArgument(models.containsKey(componentName),
                        Strings.fmt("No behavioral model found for '%s'.", componentName));
            }
        }

        String oldComponentName = options.getOldComponentName().trim();
        String newComponentName = options.getNewComponentName().trim();

        // A behavioral model named 'oldComponentName' must exist, and no model named 'newComponentName' must exist.
        Preconditions.checkArgument(models.containsKey(oldComponentName),
                Strings.fmt("Could not find a component named '%s' to rename.", oldComponentName));
        Preconditions.checkArgument(!models.containsKey(newComponentName),
                Strings.fmt("Cannot rename component '%s' to '%s' as a component with the latter name already exists.",
                        oldComponentName, newComponentName));

        // Update the mapping in 'models' for the old component name.
        PostProcessingModel oldModel = models.get(oldComponentName);
        models.remove(oldComponentName);
        models.put(newComponentName, oldModel);

        // Apply renaming to every model in 'models'.
        for (Entry<String, PostProcessingModel> entry: models.entrySet()) {
            // Get an automaton representation of the current model.
            CompactDFA<String> dfa = entry.getValue().getCompactDfa();

            // Apply renaming to every event in the automaton model. Note that, since we assume that 'newComponentName'
            // is an unused component name (checked by the preconditions above), no non-determinism can be introduced
            // as a result of this renaming. So we can stay on the level of DFAs instead of having to go to NFAs.
            CompactDFA<String> renamedDfa = AutomataLibUtil.rename(dfa,
                    eventName -> renameEvent(eventName, oldComponentName, newComponentName));

            // Convert the renamed model back to a CIF representation.
            String newModelName = entry.getValue().name.equals(oldComponentName) ? newComponentName
                    : entry.getValue().name;
            Specification renamedCif = AutomataLibToCif.fsaToCifSpecification(renamedDfa, newModelName, true);

            // Ensure that the initial location is the first location.
            Automaton automaton = CmiBasicComponentQueries.getSingleComponentWithBehavior(renamedCif);
            AutomatonExtensions.ensureInitialLocationIsFirstLocation(automaton);

            entry.setValue(
                    new PostProcessingModelCifSpec(renamedCif, newModelName, getResultStatus(entry.getValue().status)));
        }
    }

    /**
     * Given an event name {@code eventName} that complies to the CMI naming scheme, renames any occurrence of a
     * component named {@code oldComponentName} to become {@code newComponentName}.
     * 
     * @param eventName The event name subject to renaming. Should be compliant to the CMI naming scheme.
     * @param oldComponentName The old component name, whose occurrences should be rewritten.
     * @param newComponentName The new component name, replacing the old one.
     * @return A CMI-compliant event name to which the specified renaming is applied, but is otherwise identical to
     *     {@code eventName}.
     */
    private String renameEvent(String eventName, String oldComponentName, String newComponentName) {
        EventInfo info = CmiGeneralEventQueries.getEventInfo(eventName);

        // Apply renaming in the component that declares the event.
        if (info.declCompInfo.name.equals(oldComponentName)) {
            info = info.withDeclCompInfo(
                    new ComponentInfo(newComponentName, info.declCompInfo.variant, info.declCompInfo.traced));
        }

        // Apply renaming in any component at the other side of a communication.
        if (info.otherCompInfo != null && info.otherCompInfo.name.equals(oldComponentName)) {
            info = info.withOtherCompInfo(
                    new ComponentInfo(newComponentName, info.otherCompInfo.variant, info.otherCompInfo.traced));
        }

        return info.toString();
    }
}
