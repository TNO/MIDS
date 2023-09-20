package nl.tno.mids.cif.extensions;

import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.ECollections;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.declarations.Declaration;
import org.eclipse.escet.common.java.Sets;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;

@SuppressWarnings("all")
public class CifExtensions {
  /**
   * Returns a LinkedHashSet containing all automata within the complex component
   */
  public static Set<Automaton> allAutomata(final ComplexComponent comp) {
    final LinkedHashSet<Automaton> lst = CollectionLiterals.<Automaton>newLinkedHashSet();
    return CifExtensions.<LinkedHashSet<Automaton>>addAutomata(lst, comp);
  }
  
  /**
   * Adds all automata declared in the given component (recursively).
   * 
   * <p>Does not support component definition/instantiation (throws {@code UnsupportedOperationException}).</p>
   * 
   * @param automata The collection of automata, modified in place.
   * @param comp The component.
   */
  private static <C extends Collection<Automaton>> C addAutomata(final C automata, final Component comp) {
    boolean _matched = false;
    if (comp instanceof Automaton) {
      _matched=true;
      automata.add(((Automaton)comp));
    }
    if (!_matched) {
      if (comp instanceof Group) {
        _matched=true;
        final Consumer<Component> _function = (Component it) -> {
          CifExtensions.<C>addAutomata(automata, it);
        };
        ((Group)comp).getComponents().forEach(_function);
      }
    }
    if (!_matched) {
      String _name = comp.getClass().getName();
      String _plus = ("addAutomata is not defined for type" + _name);
      throw new UnsupportedOperationException(_plus);
    }
    return automata;
  }
  
  /**
   * Recursively removes all declarations in {@code component} that satisfy the given {@code predicate}.
   * 
   * @param component The component in which declarations are to be removed.
   * @param predicate The predicate that determines which declarations to remove.
   */
  public static void removeDeclarations(final ComplexComponent component, final Predicate<Declaration> predicate) {
    Set<Declaration> _list2set = Sets.<Declaration, Declaration>list2set(component.getDeclarations());
    for (final Declaration decl : _list2set) {
      boolean _apply = predicate.apply(decl);
      if (_apply) {
        component.getDeclarations().remove(decl);
      }
    }
    if ((component instanceof Group)) {
      EList<Component> _components = ((Group)component).getComponents();
      for (final Component child : _components) {
        CifExtensions.removeDeclarations(((ComplexComponent) child), predicate);
      }
    }
  }
  
  /**
   * Normalize the order of contained elements in a component.
   * 
   * <p>For complex components, sorts declarations.</p>
   * 
   * <p>For groups, additionally sorts nested components.</p>
   * 
   * <p>For automata, additionally {@link AutomatonExtensions#normalizeLocations normalizes} locations and edges.</p>
   * 
   * @param component {@link Component} containing elements to sort.
   */
  public static void normalizeOrder(final Component component) {
    if ((component instanceof ComplexComponent)) {
      CifExtensions.sortDeclarationsByType(((ComplexComponent)component));
      if ((component instanceof Group)) {
        final Comparator<Component> _function = (Component l, Component r) -> {
          return l.getName().compareTo(r.getName());
        };
        ECollections.<Component>sort(((Group)component).getComponents(), _function);
        final Comparator<Component> _function_1 = (Component l, Component r) -> {
          return l.eClass().getName().compareTo(r.eClass().getName());
        };
        ECollections.<Component>sort(((Group)component).getComponents(), _function_1);
        final Consumer<Component> _function_2 = (Component it) -> {
          CifExtensions.normalizeOrder(it);
        };
        ((Group)component).getComponents().forEach(_function_2);
      } else {
        if ((component instanceof Automaton)) {
          AutomatonExtensions.normalizeLocations(((Automaton)component));
        }
      }
    }
  }
  
  /**
   * Sort declarations in a complex component based on type and name.
   * 
   * @param complexComponent {@link ComplexComponent} containing declarations to sort.
   */
  private static void sortDeclarationsByType(final ComplexComponent component) {
    final Comparator<Declaration> _function = (Declaration l, Declaration r) -> {
      return l.getName().compareTo(r.getName());
    };
    ECollections.<Declaration>sort(component.getDeclarations(), _function);
    final Comparator<Declaration> _function_1 = (Declaration l, Declaration r) -> {
      return l.eClass().getName().compareTo(r.eClass().getName());
    };
    ECollections.<Declaration>sort(component.getDeclarations(), _function_1);
  }
}
