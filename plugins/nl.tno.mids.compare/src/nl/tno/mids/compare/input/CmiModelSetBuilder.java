/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.input;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifScopeUtils;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.common.ScopeCache;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Invariant;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.EdgeEvent;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.automata.Update;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.cif.expressions.EventExpression;
import org.eclipse.escet.cif.metamodel.cif.expressions.Expression;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.emf.EMFHelper;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.java.Lists;
import org.eclipse.escet.common.java.Maps;
import org.eclipse.escet.common.java.Strings;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cif.extensions.ExpressionExtensions;
import nl.tno.mids.cmi.api.basic.CmiBasicModifications;
import nl.tno.mids.cmi.api.basic.CmiBasicServiceFragmentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralAsyncConstraintsQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralDataQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralModifications;
import nl.tno.mids.cmi.api.general.CmiGeneralQueries;
import nl.tno.mids.cmi.api.general.CmiSubset;
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries;
import nl.tno.mids.cmi.api.split.CmiSplitServiceFragmentQueries;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.RepetitionCount;
import nl.tno.mids.compare.options.CmiCompareMode;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.options.ModelType;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;

/** A ModelSetBuilder to build a {@link ModelSet} holding models ({@link Model}) that apply to CMI specifications. */
public class CmiModelSetBuilder extends BaseModelSetBuilder {
    private final CmiCompareMode cmiCompareMode;

    private EntityType entityType;

    /**
     * @param modelSetName Name of model set to construct.
     * @param cmiCompareMode Compare mode to use for CMI models.
     * @param entityType Type of entity to compare. If the entity type is {@code null}, the model set builder will
     *     select a type based on the compare mode.
     */
    public CmiModelSetBuilder(String modelSetName, CmiCompareMode cmiCompareMode, EntityType entityType) {
        super(modelSetName);
        this.cmiCompareMode = cmiCompareMode;
        this.entityType = entityType;
    }

    @Override
    public ModelSet createModelSet(Map<Path, List<String>> descriptions) {
        // If no entity type has been selected at this point, pick a default.
        if (entityType == null) {
            entityType = new EntityType("entity", "entities");
        }
        return new ModelSet(modelSetName, models, getModelType(), entityType, descriptions);
    }

    @Override
    protected ModelType getModelType() {
        return ModelType.CMI;
    }

    public void add(Specification cifSpecification, String specificationName, List<String> warnings) {
        // Asynchronous constraints are not supported.
        Preconditions.checkArgument(!CmiGeneralAsyncConstraintsQueries.hasAsyncConstraints(cifSpecification),
                "Model (file) " + specificationName + " for model set " + modelSetName
                        + " contains asynchronuous constraints, this is not supported.");

        // Add a warning if the current model uses repetition-related data.
        if (CmiGeneralDataQueries.hasDataRepetitions(cifSpecification)) {
            warnings.add(Strings.fmt(
                    "Model '%s' in model set '%s' contains repetition-related data. Comparison support for that is experimental.",
                    specificationName, modelSetName));
        }

        // Split service fragments, if desired and possible.
        switch (cmiCompareMode) {
            case AUTOMATIC:
                // Split to service fragments if possible.
                if (CmiBasicServiceFragmentQueries.canBeSplitIntoServiceFragments(cifSpecification)) {
                    CmiBasicModifications.splitServiceFragments(cifSpecification);
                    Preconditions.checkArgument(CmiGeneralQueries.detectSubset(cifSpecification) == CmiSubset.SPLIT,
                            "Model (file) " + specificationName
                                    + " could not be split into service fragments as expected.");
                }
                break;

            case COMPONENTS: {
                // Check that is in 'basic' subset.
                CmiSubset subset = CmiGeneralQueries.detectSubset(cifSpecification);
                Preconditions.checkArgument(subset == CmiSubset.BASIC,
                        "Model (file) " + specificationName + " for model set " + modelSetName
                                + " does not contain component models that can be compared.");
                break;
            }

            case PROTOCOLS: {
                // Check that is in 'protocol' subset.
                CmiSubset subset = CmiGeneralQueries.detectSubset(cifSpecification);
                Preconditions.checkArgument(subset == CmiSubset.PROTOCOL, "Model (file) " + specificationName
                        + " for model set " + modelSetName + " does not contain a protocol that can be compared.");
                break;
            }

            case SERVICE_FRAGMENTS: {
                // Split to service fragments if possible.
                if (CmiBasicServiceFragmentQueries.canBeSplitIntoServiceFragments(cifSpecification)) {
                    CmiBasicModifications.splitServiceFragments(cifSpecification);
                }

                // Check that is in 'split' subset.
                CmiSubset subset = CmiGeneralQueries.detectSubset(cifSpecification);
                Preconditions.checkArgument(subset == CmiSubset.SPLIT,
                        "Model (file) " + specificationName + " for model set " + modelSetName
                                + " can not be split into separate service fragments that can be compared.");
                break;
            }

            default:
                throw new RuntimeException("Unknown CMI compare mode: " + cmiCompareMode + ".");
        }

        // Load based on subset.
        switch (CmiGeneralQueries.detectSubset(cifSpecification)) {
            case PROTOCOL: {
                // If entity type has not been determined yet, select protocol as type.
                if (entityType == null) {
                    entityType = new EntityType("protocol");
                }

                // Ensure exactly one protocol is present, and get its name.
                String protocolName = CmiProtocolQueries.getProtocolName(cifSpecification);

                // Preprocess structural specification.
                Specification structuralSpec = EMFHelper.deepclone(cifSpecification);
                Map<String, AnnotatedProperty<String, RepetitionCount>> repetitionInformation = encodeRepetitionInformation(
                        structuralSpec);
                Preconditions.checkArgument(!AutomatonExtensions.hasData(structuralSpec),
                        "Structural specification " + modelSetName + " contains data, which is not supported.");

                // Compute the language DFA and structural NFA.
                CompactNFA<String> languageNfa = ModelSetBuilderUtils.convertCifSpecToNfa(cifSpecification);
                CompactDFA<String> languageDfa = ModelSetBuilderUtils.convertNfaToDfa(languageNfa);
                CompactNFA<String> structureNfa = ModelSetBuilderUtils.convertCifSpecToNfa(structuralSpec);
                CompactNFA<AnnotatedProperty<String, RepetitionCount>> annotatedStructureNfa = AutomataLibUtil
                        .rename(structureNfa, repetitionInformation::get);

                // Construct and add the model.
                add(new Model(cifSpecification, languageDfa, annotatedStructureNfa, protocolName));
                break;
            }
            case BASIC: {
                // If entity type has not been determined yet, select component as type.
                if (entityType == null) {
                    entityType = new EntityType("component");
                }

                // Ensure at least one component is present.
                List<ComplexComponent> components = CmiGeneralComponentQueries
                        .getComponentsWithBehavior(cifSpecification);
                Preconditions.checkArgument(!components.isEmpty(),
                        "Model (file) " + specificationName + " for model set " + modelSetName + " contains no "
                                + entityType.getName() + " with behavior, this is not supported.");

                Preconditions.checkArgument(components.size() <= 1,
                        "Model (file) " + specificationName + " for model set " + modelSetName + " contains multiple "
                                + entityType.getPlural() + " with behavior, this is not supported.");

                String componentName = CmiGeneralComponentQueries.getComponentName(components.get(0));

                // Preprocess structural specification.
                Specification structuralSpec = EMFHelper.deepclone(cifSpecification);
                Map<String, AnnotatedProperty<String, RepetitionCount>> repetitionInformation = encodeRepetitionInformation(
                        structuralSpec);
                Preconditions.checkArgument(!AutomatonExtensions.hasData(structuralSpec),
                        "Structural specification " + modelSetName + " contains data, which is not supported.");

                // Compute the language DFA and structural NFA.
                CompactNFA<String> languageNfa = ModelSetBuilderUtils.convertCifSpecToNfa(cifSpecification);
                CompactDFA<String> languageDfa = ModelSetBuilderUtils.convertNfaToDfa(languageNfa);
                CompactNFA<String> structureNfa = ModelSetBuilderUtils.convertCifSpecToNfa(structuralSpec);
                CompactNFA<AnnotatedProperty<String, RepetitionCount>> annotatedStructureNfa = AutomataLibUtil
                        .rename(structureNfa, repetitionInformation::get);

                // Construct and add the model.
                add(new Model(cifSpecification, languageDfa, annotatedStructureNfa, componentName));
                break;
            }
            case SPLIT: {
                // If entity type has not been determined yet, select service fragments as type.
                if (entityType == null) {
                    entityType = new EntityType("service fragment");
                }

                List<Automaton> serviceFragments = CmiSplitServiceFragmentQueries.getServiceFragments(cifSpecification);
                Preconditions.checkArgument(!serviceFragments.isEmpty(),
                        "Model (file) " + specificationName + " for model set " + modelSetName + " contains no "
                                + entityType.getPlural() + " , this is not supported.");
                for (Automaton fragment: serviceFragments) {
                    // Create a specification for this service fragment only.
                    Specification originalSpec = createServiceFragmentSpecification(fragment);

                    // Preprocess structural specification.
                    Specification structuralSpec = EMFHelper.deepclone(originalSpec);
                    Map<String, AnnotatedProperty<String, RepetitionCount>> repetitionInformation = encodeRepetitionInformation(
                            structuralSpec);
                    Preconditions.checkArgument(!AutomatonExtensions.hasData(structuralSpec),
                            "Structural specification " + modelSetName + " contains data for " + entityType.getName()
                                    + " " + CifTextUtils.getAbsName(fragment, false) + ", which is not supported.");

                    // Compute the language DFA and structural NFA.
                    CompactNFA<String> languageNfa = ModelSetBuilderUtils.convertCifSpecToNfa(originalSpec);
                    CompactDFA<String> languageDfa = ModelSetBuilderUtils.convertNfaToDfa(languageNfa);
                    CompactNFA<String> structureNfa = ModelSetBuilderUtils.convertCifSpecToNfa(structuralSpec);
                    CompactNFA<AnnotatedProperty<String, RepetitionCount>> annotatedStructureNfa = AutomataLibUtil
                            .rename(structureNfa, repetitionInformation::get);

                    // Construct and add the model.
                    add(new Model(originalSpec, languageDfa, annotatedStructureNfa,
                            CifTextUtils.getAbsName(fragment, false)));
                }
                break;
            }
            default:
                Assert.fail("Not a CMI model.");
        }
    }

    /**
     * Creates a service fragment specification for the given service fragment {@code serviceFragment}, which contains
     * only the input {@link Automaton} as well as any relevant declarations, groups, etc., for the service fragment.
     * <p>
     * The returned specification does not contain service fragment exclusion invariants. Moreover, the returned
     * specification will only contain {@code serviceFragment}, meaning that any declarations and invariants in other
     * automata will be discarded.
     * </p>
     * 
     * @param serviceFragment The service fragment of which a specification is to be constructed.
     * @return The service fragment specification for {@code serviceFragment}.
     */
    private Specification createServiceFragmentSpecification(Automaton serviceFragment) {
        Preconditions.checkArgument(
                getAllVariablesDeclaredIn(serviceFragment).containsAll(getAllVariablesUsedBy(serviceFragment)),
                "The specified service fragment uses externally declared variables, which is not supported.");

        // Clone the specification.
        Specification newSpec = EMFHelper.deepclone(CifScopeUtils.getSpecRoot(serviceFragment));

        // Collect all cloned automata.
        String originalAutomatonName = CifTextUtils.getAbsName(serviceFragment, false);
        List<Automaton> clonedAutomata = new LinkedList<>();
        CifCollectUtils.collectAutomata(newSpec, clonedAutomata);

        // Remove all automata other than 'serviceFragment' from the clone.
        for (Automaton clonedAutomaton: clonedAutomata) {
            if (!CifTextUtils.getAbsName(clonedAutomaton, false).equals(originalAutomatonName)) {
                EMFHelper.removeFromParentContainment(clonedAutomaton);
            }
        }

        // Remove all event declarations from the clone other than the ones needed by 'serviceFragment'.
        CmiGeneralModifications.removeUnusedEvents(newSpec);

        // Remove all groups from the clone that are now empty due to the deletions performed above.
        CmiGeneralModifications.removeEmptyGroups(newSpec);

        return newSpec;
    }

    /**
     * Gives the set of all variables that are declared in {@code automaton}.
     * 
     * @param automaton {@link Automaton} possibly containing variable declarations.
     * @return {@link Set} of variables declared in {@code automaton}.
     */
    private Set<DiscVariable> getAllVariablesDeclaredIn(Automaton automaton) {
        return automaton.getDeclarations().stream().filter(decl -> decl instanceof DiscVariable)
                .map(decl -> (DiscVariable)decl).collect(Collectors.toSet());
    }

    /**
     * Gives the set of all variables that are used by {@code automaton} in edge guards, updates and invariants.
     * 
     * @param automaton {@link Automaton} possibly containing variable references.
     * @return {@link Set} of variables referenced in {@code automaton}.
     */
    private Set<DiscVariable> getAllVariablesUsedBy(Automaton automaton) {
        Set<DiscVariable> variables = new HashSet<>();

        // Collect all variables that are used in edge guards and updates.
        for (Location location: automaton.getLocations()) {
            for (Edge edge: location.getEdges()) {
                for (Expression guard: edge.getGuards()) {
                    variables.addAll(ExpressionExtensions.getReferencedDiscVars(guard));
                }
                for (Update update: edge.getUpdates()) {
                    variables.addAll(ExpressionExtensions.getReferencedDiscVars(update));
                }
            }
        }

        // Collect all variables that are used in invariants.
        for (Invariant invariant: automaton.getInvariants()) {
            variables.addAll(ExpressionExtensions.getReferencedDiscVars(invariant.getPredicate()));
        }

        return variables;
    }

    @Override
    protected void validate() {
        // Partition models per subset.
        Map<CmiSubset, List<Model>> modelsPerSubset = Maps.map();
        for (Model model: models.values()) {
            if (model.hasBehavior()) {
                CmiSubset subset = CmiGeneralQueries.detectSubset(model.getOriginalSpecification());
                modelsPerSubset.computeIfAbsent(subset, s -> Lists.list()).add(model);
            }
        }

        // Don't mix models of different subsets.
        if (modelsPerSubset.size() > 1) {
            String detailTxt = modelsPerSubset.entrySet().stream()
                    .map(e -> e.getKey().toString() + ": "
                            + e.getValue().stream().map(m -> m.getEntityName()).collect(Collectors.joining(", ")))
                    .collect(Collectors.joining(", "));
            Preconditions.checkState(false,
                    "Model set " + modelSetName + " mixes CMI models with different subsets: " + detailTxt);
        }
    }

    /**
     * Computes annotated properties with repetition information for every event on every edge in all automata within
     * {@code entity}, as described by {@link #encodeRepetitionInformation(Automaton)}, and returns these as a mapping
     * from (absolute) event names to the new computed annotated properties. This operation may modify {@code entity}.
     * 
     * @param entity The entity to transform.
     * @return A mapping from (absolute) event names used within {@code entity} to the computed annotated properties.
     */
    private static BiMap<String, AnnotatedProperty<String, RepetitionCount>>
            encodeRepetitionInformation(ComplexComponent entity)
    {
        // Collect all automata declared in 'entity'.
        List<Automaton> automata = Lists.list();
        CifCollectUtils.collectAutomata(entity, automata);

        // Compute repetition information of every automaton, and collect all event name mappings in a single map.
        BiMap<String, AnnotatedProperty<String, RepetitionCount>> eventNameMapping = HashBiMap.create();
        automata.forEach(automaton -> eventNameMapping.putAll(encodeRepetitionInformation(automaton)));

        return eventNameMapping;
    }

    /**
     * Computes annotated properties with repetition information for every event on all the edges in {@code automaton},
     * and returns these as a mapping from (absolute) event names used in {@code automaton} to the new computed
     * annotated properties. This operation may modify edge events within {@code automaton} and add new event
     * declarations, in order to construct such a mapping. Moreover, all repetition-related edge guards, updates and
     * declarations will be deleted.
     * <p>
     * Note: This transformation does not preserve CMI compatibility if the input automaton has repetition-related data.
     * It may introduce event names that do not comply to the CMI naming scheme. Moreover, it may cause existing
     * declarations to no longer be used. New declarations may be added by this transformation and used instead.
     * </p>
     * 
     * @param automaton The automaton to transform.
     * @return A mapping from (absolute) event names in {@code automaton} to the computed annotated properties.
     */
    private static BiMap<String, AnnotatedProperty<String, RepetitionCount>>
            encodeRepetitionInformation(Automaton automaton)
    {
        // Transform all edge events to encode repetition entries, repetition exits and repetition counts.
        BiMap<String, AnnotatedProperty<String, RepetitionCount>> newEventNameMapping = mapEdgeEvents(automaton,
                (edge, event) -> repetitionAwareEventNameFor(edge, event), true);

        // Remove all repetition-related guards and updates.
        for (Location location: automaton.getLocations()) {
            for (Edge edge: location.getEdges()) {
                edge.getGuards().removeIf(CmiGeneralDataQueries::isRepetitionGuard);
                edge.getUpdates().removeIf(CmiGeneralDataQueries::isRepetitionUpdate);
            }
        }

        // Remove all repetition-related declarations.
        automaton.getDeclarations().removeIf(
                decl -> decl instanceof DiscVariable && CmiGeneralDataQueries.isRepetitionVariable((DiscVariable)decl));

        return newEventNameMapping;
    }

    /**
     * Determines an annotated property for the specified {@code edge} and {@code event}, thereby taking into account
     * that {@code edge} may be a repetition-related edge (i.e., a repetition entry, exit or start edge).
     * 
     * @param edge The input edge, which must contain {@code event}.
     * @param event The input event, which must be an event on {@code edge} and may be {@code null} in case it is tau.
     * @return An annotated property for {@code edge} and {@code event} that encodes repetition-related information.
     */
    private static AnnotatedProperty<String, RepetitionCount> repetitionAwareEventNameFor(Edge edge, Event event) {
        Preconditions.checkArgument(EdgeExtensions.getEventDecls(edge, true).contains(event),
                "Expected the specified edge to contain the specified event.");

        String eventName;

        // Determine a repetition-aware name for the current event.
        if (event != null) {
            eventName = CifTextUtils.getAbsName(event, false);
        } else if (CmiGeneralDataQueries.isDataRepetitionEntry(edge)) {
            eventName = "repetition entry";
        } else if (CmiGeneralDataQueries.isDataRepetitionExit(edge)) {
            eventName = "repetition exit";
        } else {
            eventName = "tau";
        }

        // If 'edge' starts a repetition then also encode the repetition count as an annotation.
        Set<RepetitionCount> annotations = new LinkedHashSet<>();

        if (CmiGeneralDataQueries.isDataRepetitionStart(edge)) {
            int repetitionCount = CmiGeneralDataQueries.getDataRepetitionCount(edge);
            annotations.add(new RepetitionCount(repetitionCount));
        }

        return new AnnotatedProperty<>(eventName, annotations);
    }

    /**
     * Maps every event on every edge in {@code automaton} to a {@code T}-typed element using {@code mapper}. While
     * doing so, this operation associates every mapped element to a unique event that is used in {@code automaton}. By
     * default this is the event that is given as input to {@code mapper}. However, if this event already happens to be
     * associated to another element, then a new event will be declared and associated. In that case, {@code automaton}
     * will be modified to use this newly declared event instead of the old event. This function returns a mapping from
     * the absolute names of (possibly new declared) events used in {@code automaton}, to their associated mapped
     * elements.
     * <p>
     * This operation might thus modify {@code automaton} (and possibly its parent and sibling scopes) by adding new
     * event declarations and by replacing edge events to newly declared events. This operation does not clean up any
     * existing event declarations, meaning that there may be unused event declarations afterwards. The returned mapping
     * is complete in the sense that it contains a unique entry for every edge event that is used in {@code automaton}.
     * </p>
     * <p>
     * If {@code allowsTau} is {@code true}, then {@code mapper} may be invoked with {@code null} as the event argument,
     * to indicate a tau event.
     * </p>
     * 
     * @param <T> The element type of the mapped edge events, for which value equality must be defined.
     * @param automaton The input automaton to map and transform.
     * @param mapper The function to apply to every edge and their events. This function should never return
     *     {@code null}.
     * @param allowsTau Whether tau edges are to be handled.
     * @return A mapping from the absolute names of all events that are used in {@code automaton}, to their associated
     *     mapped elements.
     */
    private static <T> BiMap<String, T> mapEdgeEvents(Automaton automaton, BiFunction<Edge, Event, T> mapper,
            boolean allowsTau)
    {
        ScopeCache scopeCache = new ScopeCache();
        boolean updated = false;

        // Instantiate a mapping that associates elements mapped by 'mapper' to unique events used in 'automaton'.
        BiMap<T, Event> eventMap = HashBiMap.create();

        // Iterate over all edges in 'automaton'.
        for (Location location: automaton.getLocations()) {
            for (Edge edge: location.getEdges()) {
                // Iterate over all events on 'edge'.
                List<Event> events = EdgeExtensions.getEventDecls(edge, allowsTau);
                List<EdgeEvent> newEdgeEvents = new ArrayList<>(events.size());

                for (Event event: events) {
                    Preconditions.checkArgument(event != null || allowsTau, "Expected not to encounter tau edges.");

                    // Determine the element that is mapped for 'edge' and 'event'.
                    T element = mapper.apply(edge, event);
                    Preconditions.checkNotNull(element, "Expected mapped elements to be non-null.");

                    // Determine the event that should be associated to 'element'.
                    Event associatedEvent;

                    if (eventMap.containsKey(element)) {
                        // If 'element' has been encountered before, then reuse the associated event used earlier.
                        associatedEvent = eventMap.get(element);
                    } else if (event != null && !eventMap.containsValue(event)) {
                        // If 'event' has not been associated before and is not tau, then associate it to 'element'.
                        associatedEvent = event;
                        eventMap.put(element, associatedEvent);
                    } else {
                        // Otherwise, declare a new event that is to be associated to 'element'.
                        ComplexComponent eventScope = event == null ? automaton
                                : (ComplexComponent)CifScopeUtils.getScope(event);

                        // Determine a fresh name for the new event.
                        String oldName = event == null ? "tau" : event.getName();
                        Set<String> usedNames = CifScopeUtils.getSymbolNamesForScope(eventScope, scopeCache);
                        String newName = CifScopeUtils.getUniqueName(oldName, usedNames, ImmutableSet.of());

                        // Declare the new event.
                        associatedEvent = CifConstructors.newEvent();
                        associatedEvent.setName(newName);
                        eventScope.getDeclarations().add(associatedEvent);
                        scopeCache.get(eventScope).add(newName);

                        // Associate the new event to 'element'.
                        eventMap.put(element, associatedEvent);
                        updated = true;
                    }

                    // Construct a new edge event for 'associatedEvent'. This is much easier then determining whether
                    // existing edge events should be kept or not. We just replace them all by fresh edge events.
                    EventExpression newEventExp = CifConstructors.newEventExpression();
                    newEventExp.setType(CifConstructors.newBoolType());
                    newEventExp.setEvent(associatedEvent);
                    EdgeEvent newEdgeEvent = CifConstructors.newEdgeEvent();
                    newEdgeEvent.setEvent(newEventExp);
                    newEdgeEvents.add(newEdgeEvent);
                }

                // Replace all existing edge events by the new edge events.
                edge.getEvents().clear();
                edge.getEvents().addAll(newEdgeEvents);
            }
        }

        if (updated) {
            AutomatonExtensions.updateAlphabet(automaton);
        }

        // Construct a mapping from absolute names of all events used in 'automaton' to their associated mapped element.
        BiMap<String, T> eventNameMap = HashBiMap.create(eventMap.size());

        for (Entry<T, Event> entry: eventMap.entrySet()) {
            String absoluteEventName = CifTextUtils.getAbsName(entry.getValue(), false);
            eventNameMap.put(absoluteEventName, entry.getKey());
        }

        return eventNameMap;
    }
}
