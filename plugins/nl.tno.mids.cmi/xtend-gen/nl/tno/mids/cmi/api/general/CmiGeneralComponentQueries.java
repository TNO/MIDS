package nl.tno.mids.cmi.api.general;

import java.util.List;
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries;
import nl.tno.mids.cmi.api.info.ComponentInfo;
import nl.tno.mids.cmi.api.protocol.CmiProtocolComponentQueries;
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries;
import nl.tno.mids.cmi.api.split.CmiSplitComponentQueries;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.escet.cif.metamodel.cif.ComplexComponent;
import org.eclipse.escet.cif.metamodel.cif.Component;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiGeneralComponentQueries {
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
    boolean _isProtocol = CmiProtocolQueries.isProtocol(((ComplexComponent) object));
    if (_isProtocol) {
      return false;
    }
    EObject _eContainer = object.eContainer();
    return (_eContainer instanceof Specification);
  }
  
  /**
   * Get the component in which the given object is found. If the given object is a component, it is itself returned.
   * 
   * <p>Components are CIF automata or CIF groups in the root of the model.</p>
   * 
   * <p>Protocols are not components, even if they are represented as CIF automata in the root of the model. Objects
   * contained in protocols are not contained in any component.</p>
   * 
   * @param object The object.
   * @return The component.
   */
  protected static ComplexComponent getComponent(final PositionObject object) {
    final Specification model = CmiGeneralQueries.getModel(object);
    final CmiSubset subset = CmiGeneralQueries.detectSubset(model);
    return CmiGeneralComponentQueries.getComponent(object, subset);
  }
  
  /**
   * Get the component in which the given object is found, assuming a given API subset.
   * If the given object is a component, it is itself returned.
   * 
   * <p>Components are CIF automata or CIF groups in the root of the model.</p>
   * 
   * <p>Protocols are not components, even if they are represented as CIF automata in the root of the model. Objects
   * contained in protocols are not contained in any component.</p>
   * 
   * @param object The object.
   * @param subset The API subset of the model.
   * @return The component.
   */
  protected static ComplexComponent getComponent(final PositionObject object, final CmiSubset subset) {
    if (subset != null) {
      switch (subset) {
        case PROTOCOL:
          return CmiProtocolComponentQueries.getComponent(object);
        case BASIC:
          return CmiBasicComponentQueries.getComponent(object);
        case SPLIT:
          return CmiSplitComponentQueries.getComponent(object);
        default:
          throw new RuntimeException("Unknown subset");
      }
    } else {
      throw new RuntimeException("Unknown subset");
    }
  }
  
  /**
   * Get the components of the model.
   * 
   * <p>Components are CIF automata or CIF groups in the root of the model.</p>
   * 
   * <p>Protocols are not components, even if they are represented as CIF automata in the root of the model.</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<ComplexComponent> getComponents(final Specification model) {
    final Function1<Component, Boolean> _function = (Component it) -> {
      return Boolean.valueOf(CmiGeneralComponentQueries.isComponent(it));
    };
    final Function1<Component, ComplexComponent> _function_1 = (Component it) -> {
      return ((ComplexComponent) it);
    };
    return IterableExtensions.<ComplexComponent>toList(IterableExtensions.<Component, ComplexComponent>map(IterableExtensions.<Component>filter(model.getComponents(), _function), _function_1));
  }
  
  /**
   * Get the components with behavior of the model.
   * 
   * <p>For 'basic' models, this includes all components that are CIF automata. For 'split' models, this includes all
   * components (CIF groups) that contain service fragments (CIF automata). Models in the 'protocol' subset do not
   * have components with behavior.</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<ComplexComponent> getComponentsWithBehavior(final Specification model) {
    CmiSubset _detectSubset = CmiGeneralQueries.detectSubset(model);
    if (_detectSubset != null) {
      switch (_detectSubset) {
        case PROTOCOL:
          return CollectionLiterals.<ComplexComponent>newArrayList();
        case BASIC:
          return CollectionLiterals.<ComplexComponent>newArrayList(((ComplexComponent[])Conversions.unwrapArray(CmiBasicComponentQueries.getComponentsWithBehavior(model), ComplexComponent.class)));
        case SPLIT:
          return CollectionLiterals.<ComplexComponent>newArrayList(((ComplexComponent[])Conversions.unwrapArray(CmiSplitComponentQueries.getComponentsWithBehavior(model), ComplexComponent.class)));
        default:
          throw new RuntimeException("Unknown subset");
      }
    } else {
      throw new RuntimeException("Unknown subset");
    }
  }
  
  /**
   * Get the components without behavior of the model.
   * 
   * <p>For 'basic' models, this includes all components that are CIF groups. For 'split' models, this includes all
   * components (CIF groups) that don't contain service fragments (CIF automata). For 'protocol' models, this includes
   * all components.</p>
   * 
   * @param model The model.
   * @return The components.
   */
  public static List<Group> getComponentsWithoutBehavior(final Specification model) {
    CmiSubset _detectSubset = CmiGeneralQueries.detectSubset(model);
    if (_detectSubset != null) {
      switch (_detectSubset) {
        case PROTOCOL:
          return CmiProtocolComponentQueries.getComponentsWithoutBehavior(model);
        case BASIC:
          return CmiBasicComponentQueries.getComponentsWithoutBehavior(model);
        case SPLIT:
          return CmiSplitComponentQueries.getComponentsWithoutBehavior(model);
        default:
          throw new RuntimeException("Unknown subset");
      }
    } else {
      throw new RuntimeException("Unknown subset");
    }
  }
  
  /**
   * Get the name of a component.
   * 
   * @param component The component.
   * @return The name of the component.
   */
  public static String getComponentName(final ComplexComponent component) {
    Assert.check(CmiGeneralComponentQueries.isComponent(component));
    EObject _eContainer = component.eContainer();
    Assert.check((_eContainer instanceof Specification));
    return component.getName();
  }
  
  /**
   * Returns information about the component, i.e. information about the parts of the component name.
   * 
   * @param component The component.
   * @return The component information.
   */
  public static ComponentInfo getComponentInfo(final ComplexComponent component) {
    String _componentName = CmiGeneralComponentQueries.getComponentName(component);
    return new ComponentInfo(_componentName);
  }
}
