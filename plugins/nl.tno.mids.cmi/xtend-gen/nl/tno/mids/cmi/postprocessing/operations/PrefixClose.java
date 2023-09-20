package nl.tno.mids.cmi.postprocessing.operations;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.util.automata.fsa.DFAs;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtend.lib.annotations.Accessors;

@Accessors
@SuppressWarnings("all")
public class PrefixClose extends PostProcessingOperation<PrefixCloseOptions> {
  @Override
  public PostProcessingPreconditionSubset getPreconditionSubset() {
    return new PostProcessingPreconditionSubset(Boolean.valueOf(false), null);
  }
  
  @Override
  public PostProcessingResultSubset getResultSubset() {
    return new PostProcessingResultSubset(Boolean.valueOf(false), null);
  }
  
  @Override
  public void applyOperation(final Map<String, PostProcessingModel> models, final Set<String> selectedComponents, final Path relativeResolvePath, final IProgressMonitor monitor) {
    monitor.subTask("Making prefix closed");
    for (final String component : selectedComponents) {
      {
        final PostProcessingModel model = models.get(component);
        this.getPreconditionSubset().ensureSubset(model);
        final CompactDFA<String> dfa = model.getCompactDfa();
        boolean _isPrefixClosed = DFAs.<Integer, String>isPrefixClosed(dfa, dfa.getInputAlphabet());
        boolean _not = (!_isPrefixClosed);
        if (_not) {
          AutomataLibUtil.prefixClose(dfa);
          PostProcessingStatus _resultStatus = this.getResultStatus(model.status);
          PostProcessingModelCompactDfa _postProcessingModelCompactDfa = new PostProcessingModelCompactDfa(dfa, model.name, _resultStatus);
          models.put(component, _postProcessingModelCompactDfa);
        }
      }
    }
  }
  
  public PrefixClose(final PrefixCloseOptions options) {
    super(options);
  }
}
