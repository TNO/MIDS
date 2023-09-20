package nl.tno.mids.cmi.postprocessing;

import com.google.common.base.Preconditions;
import java.util.Map;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import nl.tno.mids.automatalib.extensions.cif.CifToAutomataLib;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.escet.cif.metamodel.cif.Specification;

@SuppressWarnings("all")
public class PostProcessingModelCifSpec extends PostProcessingModel {
  private final Specification specification;
  
  public PostProcessingModelCifSpec(final Specification specification, final String name, final PostProcessingStatus status) {
    super(name, status);
    this.specification = specification;
  }
  
  @Override
  public Specification getCifSpec() {
    return this.specification;
  }
  
  @Override
  public CompactDFA<String> getCompactDfa() {
    Preconditions.checkState(this.status.dataIsNotPresent());
    final Map<String, CompactDFA<String>> dfas = CifToAutomataLib.cifSpecificationToCompactDfas(this.specification, false);
    int _size = dfas.size();
    boolean _equals = (_size == 1);
    Preconditions.checkState(_equals);
    return dfas.entrySet().iterator().next().getValue();
  }
  
  @Override
  public PostProcessingModel recategorizeAsNoData() {
    Preconditions.checkArgument(this.status.dataIsPresent());
    boolean _tauIsPresent = this.status.tauIsPresent();
    PostProcessingStatus _postProcessingStatus = new PostProcessingStatus(false, _tauIsPresent);
    return new PostProcessingModelCifSpec(this.specification, this.name, _postProcessingStatus);
  }
  
  @Override
  public PostProcessingModel recategorizeAsNoTau() {
    Preconditions.checkArgument(this.status.tauIsPresent());
    boolean _dataIsPresent = this.status.dataIsPresent();
    PostProcessingStatus _postProcessingStatus = new PostProcessingStatus(_dataIsPresent, false);
    return new PostProcessingModelCifSpec(this.specification, this.name, _postProcessingStatus);
  }
}
