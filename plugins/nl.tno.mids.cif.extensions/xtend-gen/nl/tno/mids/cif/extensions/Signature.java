package nl.tno.mids.cif.extensions;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.automata.Location;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.xtend.lib.annotations.Data;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

@Data
@SuppressWarnings("all")
public class Signature {
  private final Map<Event, Location> outgoingTransitions;
  
  private final boolean accepts;
  
  public Signature(final Location q) {
    try {
      HashMap<Event, Location> _hashMap = new HashMap<Event, Location>();
      this.outgoingTransitions = _hashMap;
      EList<Edge> _edges = q.getEdges();
      for (final Edge edge : _edges) {
        {
          final Location oldTrans = this.outgoingTransitions.put(EdgeExtensions.getEventDecl(edge, true), EdgeExtensions.getDestination(edge));
          if ((oldTrans != null)) {
            Event _eventDecl = EdgeExtensions.getEventDecl(edge, true);
            throw new NonDeterministicChoiceException(q, _eventDecl);
          }
        }
      }
      boolean _xifexpression = false;
      boolean _isEmpty = q.getEdges().isEmpty();
      if (_isEmpty) {
        _xifexpression = true;
      } else {
        _xifexpression = false;
      }
      this.accepts = _xifexpression;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Override
  @Pure
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.outgoingTransitions== null) ? 0 : this.outgoingTransitions.hashCode());
    return prime * result + (this.accepts ? 1231 : 1237);
  }
  
  @Override
  @Pure
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Signature other = (Signature) obj;
    if (this.outgoingTransitions == null) {
      if (other.outgoingTransitions != null)
        return false;
    } else if (!this.outgoingTransitions.equals(other.outgoingTransitions))
      return false;
    if (other.accepts != this.accepts)
      return false;
    return true;
  }
  
  @Override
  @Pure
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("outgoingTransitions", this.outgoingTransitions);
    b.add("accepts", this.accepts);
    return b.toString();
  }
  
  @Pure
  public Map<Event, Location> getOutgoingTransitions() {
    return this.outgoingTransitions;
  }
  
  @Pure
  public boolean isAccepts() {
    return this.accepts;
  }
}
