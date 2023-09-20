package nl.tno.mids.cif.extensions;

import org.eclipse.escet.cif.common.CifLocationUtils;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;

@SuppressWarnings("all")
public class NonDeterministicChoiceException extends Exception {
  public NonDeterministicChoiceException(final Location state, final Event event) {
    super(
      ((((("State " + state.getName()) + " in automaton ") + CifLocationUtils.getAutomaton(state).getName()) + 
        " has multiple outgoing transitions for event ") + event.getName()));
  }
}
