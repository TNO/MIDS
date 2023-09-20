package nl.tno.mids.cmi.postprocessing;

import com.google.common.base.Preconditions;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.escet.cif.metamodel.cif.Specification;

@SuppressWarnings("all")
public class PostProcessingModelCompactDfa extends PostProcessingModel {
  private final CompactDFA<String> dfa;
  
  public PostProcessingModelCompactDfa(final CompactDFA<String> dfa, final String name, final PostProcessingStatus status) {
    super(name, status);
    this.dfa = dfa;
    Preconditions.checkArgument(status.dataIsNotPresent());
  }
  
  @Override
  public Specification getCifSpec() {
    return AutomataLibToCif.<Integer, Integer, Void, CompactDFA<String>>fsaToCifSpecification(this.dfa, this.name, true);
  }
  
  @Override
  public CompactDFA<String> getCompactDfa() {
    Preconditions.checkState(this.status.dataIsNotPresent());
    return this.dfa;
  }
  
  @Override
  public PostProcessingModel recategorizeAsNoData() {
    throw new UnsupportedOperationException(
      "Can\'t re-categorize. Must already have no data, as CompactDFA cannot represent data.");
  }
  
  @Override
  public PostProcessingModel recategorizeAsNoTau() {
    Preconditions.checkArgument(this.status.tauIsPresent());
    boolean _dataIsPresent = this.status.dataIsPresent();
    PostProcessingStatus _postProcessingStatus = new PostProcessingStatus(_dataIsPresent, false);
    return new PostProcessingModelCompactDfa(this.dfa, this.name, _postProcessingStatus);
  }
}
