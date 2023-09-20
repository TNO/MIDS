package nl.tno.mids.cmi.api.split;

import java.util.List;
import java.util.function.Consumer;
import org.eclipse.escet.cif.metamodel.cif.Group;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.java.CifConstructors;
import org.eclipse.escet.common.java.Assert;

@SuppressWarnings("all")
public class CmiSplitModifications {
  /**
   * Move service fragments starting with event subscription or event unsubscription to a subgroup.
   * 
   * @param component Component containing service fragments to move.
   */
  public static boolean groupEventSubUnsubFragments(final Group component) {
    boolean _xblockexpression = false;
    {
      boolean _isComponent = CmiSplitComponentQueries.isComponent(component);
      String _name = component.getName();
      String _plus = ("Group " + _name);
      String _plus_1 = (_plus + " is not a component.");
      Assert.check(_isComponent, _plus_1);
      final List<Automaton> serviceFragments = CmiSplitServiceFragmentQueries.getServiceFragments(component);
      final Group newGroup = CifConstructors.newGroup();
      newGroup.setName("EventSubscriptionsAndUnsubscriptions");
      final Consumer<Automaton> _function = (Automaton serviceFragment) -> {
        boolean _isEventSubscriptionOrUnsubscriptionServiceFragment = CmiSplitServiceFragmentQueries.isEventSubscriptionOrUnsubscriptionServiceFragment(serviceFragment);
        if (_isEventSubscriptionOrUnsubscriptionServiceFragment) {
          newGroup.getComponents().add(serviceFragment);
        }
      };
      serviceFragments.forEach(_function);
      boolean _xifexpression = false;
      boolean _isEmpty = newGroup.getComponents().isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        _xifexpression = component.getComponents().add(newGroup);
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
}
