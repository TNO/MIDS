package nl.tno.mids.cmi.postprocessing.operations;

import java.util.HashMap;
import nl.tno.mids.cmi.api.general.CmiGeneralAsyncPatternQueries;
import nl.tno.mids.cmi.api.info.EventInfo;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Pure;

/**
 * Add constraints to the models to enforce asynchronous patterns (e.g. requests/replies).
 */
@Accessors
@SuppressWarnings("all")
public class AddAsyncPatternConstraints extends AddAsyncPatternConstraintsBase<AddAsyncPatternConstraintsOptions> {
  private final HashMap<Event, EventInfo> cache = CollectionLiterals.<Event, EventInfo>newHashMap();
  
  @Override
  public String getTaskName() {
    return "Add asynchronous pattern constraints (general)";
  }
  
  @Override
  public boolean isMatchingAsyncPatternEnd(final Edge startEdge, final Edge endEdge) {
    return CmiGeneralAsyncPatternQueries.isAsyncPatternPair(this.cache, startEdge, endEdge);
  }
  
  @Override
  public boolean isAsyncPatternStart(final Edge edge) {
    return CmiGeneralAsyncPatternQueries.isAsyncPatternStart(edge);
  }
  
  public AddAsyncPatternConstraints(final AddAsyncPatternConstraintsOptions options) {
    super(options);
  }
  
  @Pure
  public HashMap<Event, EventInfo> getCache() {
    return this.cache;
  }
}
