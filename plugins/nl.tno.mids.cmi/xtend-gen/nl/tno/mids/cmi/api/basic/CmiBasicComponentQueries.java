package nl.tno.mids.cmi.api.basic;

import com.google.common.collect.Iterables;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiBasicComponentQueries {
  /**
   * Does the given object represent a component?
   * 
   * @param object The object.
   * @return {@code true} if it represents a component, {@code false} otherwise.
   */
  public static boolean isComponent(final PositionObject object) {
    if ((!((object instanceof Automaton) || (object instanceof Group)))) {
      return false;
    }
    EObject _eContainer = object.eContainer();
    return (_eContainer instanceof Specification);
  }
  
  /**
   * Get the component in which the given object is found. If the given object is a component, it is itself returned.
   * 
   * @param object The object.
   * @return The component.
   */
  public static ComplexComponent getComponent(final PositionObject object) {
    PositionObject result = object;
    if (((result instanceof Specification) || (result == null))) {
      throw new RuntimeException(("Given object not contained in a component: " + object));
    }
    while ((!CmiBasicComponentQueries.isComponent(result))) {
      {
        EObject _eContainer = result.eContainer();
        result = ((PositionObject) _eContainer);
        if (((result instanceof Specification) || (result == null))) {
          throw new RuntimeException(("Given object not contained in a component: " + object));
        }
      }
    }
    return ((ComplexComponent) result);
  }
  
  /**
   * Get the components of the model.
   * 
   * <p>Components are CIF automata or CIF groups in the root of the model.</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<ComplexComponent> getComponents(final Specification model) {
    final Function1<Component, Boolean> _function = (Component it) -> {
      return Boolean.valueOf(CmiBasicComponentQueries.isComponent(it));
    };
    final Function1<Component, ComplexComponent> _function_1 = (Component it) -> {
      return ((ComplexComponent) it);
    };
    return IterableExtensions.<ComplexComponent>toList(IterableExtensions.<Component, ComplexComponent>map(IterableExtensions.<Component>filter(model.getComponents(), _function), _function_1));
  }
  
  /**
   * Get the components with behavior of the model.
   * 
   * <p>This includes all components that are CIF automata.</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<Automaton> getComponentsWithBehavior(final Specification model) {
    final List<ComplexComponent> components = CmiBasicComponentQueries.getComponents(model);
    return IterableExtensions.<Automaton>toList(Iterables.<Automaton>filter(components, Automaton.class));
  }
  
  /**
   * Get the single component with behavior of the model. There should be exactly one such component.
   * 
   * <p>This is the only component that is a CIF automaton.</p>
   * 
   * @param model The model.
   * @return The component.
   */
  public static Automaton getSingleComponentWithBehavior(final Specification model) {
    final List<Automaton> componentList = CmiBasicComponentQueries.getComponentsWithBehavior(model);
    int _size = componentList.size();
    boolean _equals = (_size == 1);
    Assert.check(_equals);
    return IterableExtensions.<Automaton>head(componentList);
  }
  
  /**
   * Get the components without behavior of the model.
   * 
   * <p>This includes all components that are CIF groups.</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<Group> getComponentsWithoutBehavior(final Specification model) {
    final List<ComplexComponent> components = CmiBasicComponentQueries.getComponents(model);
    return IterableExtensions.<Group>toList(Iterables.<Group>filter(components, Group.class));
  }
}
