package nl.tno.mids.cmi.api.general;

import com.google.common.base.Predicate;
import java.util.List;
import java.util.Set;
import nl.tno.mids.cif.extensions.CifExtensions;
import org.eclipse.escet.cif.common.CifCollectUtils;
import org.eclipse.escet.cif.common.CifEventUtils;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiGeneralModifications {
  /**
   * Recursively removes all CIF groups within the given CIF {@link ComplexComponent} that are empty (i.e., that do
   * not have any declarations, nested sub-components, etc.). Note that the group emptiness check only considers CIF
   * constructs that are in-scope for CMI (so no component definition/instantiation, equations, etc.).
   * 
   * <p>
   * If {@code component} is itself an empty CIF group, it is not deleted.
   * </p>
   * 
   * @param component The component from which empty CIF groups are to be removed.
   */
  public static void removeEmptyGroups(final ComplexComponent component) {
    if ((component instanceof Group)) {
      Set<Component> _list2set = Sets.<Component, Component>list2set(((Group)component).getComponents());
      for (final Component child : _list2set) {
        {
          CmiGeneralModifications.removeEmptyGroups(((ComplexComponent) child));
          if ((child instanceof Group)) {
            if (((((Group)child).getDeclarations().isEmpty() && ((Group)child).getInvariants().isEmpty()) && ((Group)child).getComponents().isEmpty())) {
              ((Group)component).getComponents().remove(child);
            }
          }
        }
      }
    }
  }
  
  /**
   * Remove all events from a model that are not used in the model.
   * 
   * <p>The alphabet of all the automata are used to determine whether events are used or not.</p>
   * 
   * @param model Model from which to remove unused events.
   */
  public static void removeUnusedEvents(final Specification model) {
    final List<Automaton> automata = CollectionLiterals.<Automaton>newArrayList();
    CifCollectUtils.<List<Automaton>>collectAutomata(model, automata);
    final Function1<Automaton, Set<Event>> _function = (Automaton it) -> {
      return CifEventUtils.getAlphabet(it);
    };
    final Iterable<Event> events = IterableExtensions.<Automaton, Event>flatMap(automata, _function);
    final Predicate<Declaration> _function_1 = (Declaration decl) -> {
      return ((decl instanceof Event) && (!IterableExtensions.contains(events, decl)));
    };
    CifExtensions.removeDeclarations(model, _function_1);
  }
}
