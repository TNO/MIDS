package nl.tno.mids.cmi.api.basic;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import org.eclipse.escet.cif.common.CifTextUtils;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.DiscVariable;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.emf.EMFHelper;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiBasicModifications {
  /**
   * Split component automata into separate service fragment automata, contained in a group that represents the
   * entire component.
   * 
   * <p>
   * Note that:
   * <ul>
   * <li>The resulting service fragment automata collectively do not have the same behavior as the original component
   * automaton, as the service fragments are no longer guaranteed to be mutually exclusive. They may interleave and
   * multiple of them may be 'active' at the same time.</li>
   * <li>The alphabets of the service fragment automata may be subsets of the original component automaton.</li>
   * <li>Variables shared between service fragments will no longer be shared between service fragment automata.
   * Service fragments access local copies of variables instead. In particular, asynchronous constraints no longer
   * constrain behavior as intended.</li>
   * </ul>
   * This transformation is intended for operations such a comparison or visualization where the state space of the
   * complete specification is not relevant.
   * </p>
   * 
   * @param model The specification containing components to be split into service fragment automata.
   *      Is modified in-place.
   */
  public static void splitServiceFragments(final Specification model) {
    final List<Automaton> automata = CmiBasicComponentQueries.getComponentsWithBehavior(model);
    final HashMap<Automaton, Group> automataGroupMap = CollectionLiterals.<Automaton, Group>newHashMap();
    for (final Automaton automaton : automata) {
      {
        final Group group = CifConstructors.newGroup();
        group.setName(automaton.getName());
        model.getComponents().add(group);
        final ArrayList<Event> eventDecls = CollectionLiterals.<Event>newArrayList();
        Iterables.<Event>addAll(eventDecls, Iterables.<Event>filter(automaton.getDeclarations(), Event.class));
        group.getDeclarations().addAll(eventDecls);
        group.getInvariants().addAll(automaton.getInvariants());
        automataGroupMap.put(automaton, group);
      }
    }
    for (final Automaton automaton_1 : automata) {
      {
        final Consumer<Event> _function = (Event initialEvent) -> {
          final String eventName = CifTextUtils.getAbsName(initialEvent, false);
          Assert.check(CmiGeneralEventQueries.isValidEventName(eventName));
          final Group automatonGroup = automataGroupMap.get(automaton_1);
          automatonGroup.getComponents().add(CmiBasicModifications.splitServiceFragment(eventName, automaton_1));
        };
        CmiBasicServiceFragmentQueries.getServiceFragmentInitialEvents(automaton_1).forEach(_function);
        model.getComponents().remove(automaton_1);
      }
    }
  }
  
  /**
   * Splits a single service fragment, creating a dedicated automaton for it.
   * 
   * <p>Note that references to declarations contained in the {@code scourceAutomaton} will be updated to refer to
   * copies contained in the resulting automaton. This ensures that local declarations shared between service
   * fragments will not be shared between service fragment automata.</p>
   * 
   * @param initialEventName The name of the initial event of the service fragment to split.
   * @param sourceAutomaton Automaton currently containing the service fragment.
   * @return Automaton containing the split service fragment.
   */
  private static Automaton splitServiceFragment(final String initialEventName, final Automaton sourceAutomaton) {
    final Automaton newAutomaton = EMFHelper.<Automaton>deepclone(sourceAutomaton);
    final Location initialLocation = AutomatonExtensions.initialLocation(newAutomaton);
    final Predicate<Edge> _function = (Edge it) -> {
      boolean _equals = CifTextUtils.getAbsName(EdgeExtensions.getEventDecl(it, false), false).equals(initialEventName);
      return (!_equals);
    };
    initialLocation.getEdges().removeIf(_function);
    AutomatonExtensions.removeUnreachableLocations(newAutomaton);
    final Set<DiscVariable> usedVariables = AutomatonExtensions.getReferencedDiscVars(newAutomaton);
    final ArrayList<DiscVariable> notUsedVariables = CollectionLiterals.<DiscVariable>newArrayList();
    final Function1<DiscVariable, Boolean> _function_1 = (DiscVariable it) -> {
      boolean _contains = usedVariables.contains(it);
      return Boolean.valueOf((!_contains));
    };
    Iterables.<DiscVariable>addAll(notUsedVariables, IterableExtensions.<DiscVariable>filter(Iterables.<DiscVariable>filter(newAutomaton.getDeclarations(), DiscVariable.class), _function_1));
    final Consumer<DiscVariable> _function_2 = (DiscVariable it) -> {
      EMFHelper.removeFromParentContainment(it);
    };
    notUsedVariables.forEach(_function_2);
    AutomatonExtensions.renumberLocations(newAutomaton);
    newAutomaton.setName(initialEventName.replace(".", "_"));
    return newAutomaton;
  }
}
