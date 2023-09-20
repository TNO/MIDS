package nl.tno.mids.cmi.postprocessing;

import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.escet.cif.metamodel.cif.Specification;

@SuppressWarnings("all")
public abstract class PostProcessingModel {
  public final String name;
  
  public final PostProcessingStatus status;
  
  public PostProcessingModel(final String name, final PostProcessingStatus status) {
    this.name = name;
    this.status = status;
  }
  
  public abstract Specification getCifSpec();
  
  public abstract CompactDFA<String> getCompactDfa();
  
  /**
   * Re-categorizes this model as having no data. Must only be invoked if currently may have data.
   */
  public abstract PostProcessingModel recategorizeAsNoData();
  
  /**
   * Re-categorizes this model as having no tau. Must only be invoked if currently may have tau events.
   */
  public abstract PostProcessingModel recategorizeAsNoTau();
}
