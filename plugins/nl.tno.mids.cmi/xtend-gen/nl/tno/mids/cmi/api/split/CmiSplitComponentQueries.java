package nl.tno.mids.cmi.api.split;

import com.google.common.collect.Iterables;
import java.util.List;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiSplitComponentQueries {
  /**
   * Does the given object represent a component?
   * 
   * @param object The object.
   * @return {@code true} if it represents a component, {@code false} otherwise.
   */
  public static boolean isComponent(final PositionObject object) {
    if ((!(object instanceof Group))) {
      return false;
    }
    EObject _eContainer = object.eContainer();
    return (_eContainer instanceof Specification);
  }
  
  /**
   * Get the component in which the given object is found. If the given object is a component, it is itself returned.
   * 
   * <p>Components are CIF groups in the root of the model.</p>
   * 
   * @param object The object.
   * @return The component.
   */
  public static Group getComponent(final PositionObject object) {
    PositionObject result = object;
    if (((result instanceof Specification) || (result == null))) {
      throw new RuntimeException(("Given object not contained in a component: " + object));
    }
    while ((!CmiSplitComponentQueries.isComponent(result))) {
      {
        EObject _eContainer = result.eContainer();
        result = ((PositionObject) _eContainer);
        if (((result instanceof Specification) || (result == null))) {
          throw new RuntimeException(("Given object not contained in a component: " + object));
        }
      }
    }
    return ((Group) result);
  }
  
  /**
   * Get the components of the model.
   * 
   * <p>Components are CIF groups in the root of the model.</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<Group> getComponents(final Specification model) {
    final Function1<Component, Boolean> _function = (Component it) -> {
      return Boolean.valueOf(CmiSplitComponentQueries.isComponent(it));
    };
    return IterableExtensions.<Group>toList(Iterables.<Group>filter(IterableExtensions.<Component>filter(model.getComponents(), _function), Group.class));
  }
  
  /**
   * Get the components with behavior of the model.
   * 
   * <p>This includes all components (CIF groups) that contain service fragments (CIF automata).</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<Group> getComponentsWithBehavior(final Specification model) {
    final List<Group> components = CmiSplitComponentQueries.getComponents(model);
    final Function1<Group, Boolean> _function = (Group it) -> {
      boolean _isEmpty = CmiSplitServiceFragmentQueries.getServiceFragments(it).isEmpty();
      return Boolean.valueOf((!_isEmpty));
    };
    return IterableExtensions.<Group>toList(IterableExtensions.<Group>filter(components, _function));
  }
  
  /**
   * Get the components without behavior of the model.
   * 
   * <p>This includes all components (CIF groups) that don't contain service fragments (CIF automata).</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<Group> getComponentsWithoutBehavior(final Specification model) {
    final List<Group> components = CmiSplitComponentQueries.getComponents(model);
    final Function1<Group, Boolean> _function = (Group it) -> {
      return Boolean.valueOf(CmiSplitServiceFragmentQueries.getServiceFragments(it).isEmpty());
    };
    return IterableExtensions.<Group>toList(IterableExtensions.<Group>filter(components, _function));
  }
}
