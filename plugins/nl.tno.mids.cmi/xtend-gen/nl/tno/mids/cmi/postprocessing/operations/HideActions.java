package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Objects;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Alphabet;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtend.lib.annotations.Accessors;

/**
 * Hide actions matching a given regular expression.
 */
@Accessors
@SuppressWarnings("all")
public class HideActions extends PostProcessingOperation<HideActionsOptions> {
  @Override
  public PostProcessingPreconditionSubset getPreconditionSubset() {
    return new PostProcessingPreconditionSubset(Boolean.valueOf(false), null);
  }
  
  @Override
  public PostProcessingResultSubset getResultSubset() {
    return new PostProcessingResultSubset(Boolean.valueOf(false), Boolean.valueOf(false));
  }
  
  @Override
  public void applyOperation(final Map<String, PostProcessingModel> models, final Set<String> selectedComponents, final Path relativeResolvePath, final IProgressMonitor monitor) {
    monitor.subTask("Hiding actions");
    final Pattern pattern = Pattern.compile(this.options.pattern);
    for (final String component : selectedComponents) {
      {
        final PostProcessingModel componentModel = models.get(component);
        this.getPreconditionSubset().ensureSubset(componentModel);
        final CompactDFA<String> previousDfa = componentModel.getCompactDfa();
        final Integer initialState = previousDfa.getInitialState();
        final Collection<String> serviceFragmentEvents = previousDfa.getLocalInputs(initialState);
        Collection<Integer> _states = previousDfa.getStates();
        for (final Integer state : _states) {
          Collection<String> _localInputs = previousDfa.getLocalInputs(state);
          for (final String input : _localInputs) {
            Integer _successor = previousDfa.getSuccessor(state, input);
            boolean _equals = Objects.equal(_successor, initialState);
            if (_equals) {
              serviceFragmentEvents.add(input);
            }
          }
        }
        final Function<Alphabet<String>, CompactNFA<String>> _function = (Alphabet<String> a) -> {
          return new CompactNFA<String>(a);
        };
        final Function<String, String> _function_1 = (String t) -> {
          String _xifexpression = null;
          if ((pattern.matcher(t).matches() && (!serviceFragmentEvents.contains(t)))) {
            _xifexpression = "tau";
          } else {
            _xifexpression = t;
          }
          return _xifexpression;
        };
        final CompactNFA<String> newNfa = AutomataLibUtil.<Integer, Integer, String, String, CompactDFA<String>, CompactNFA<String>>rename(previousDfa, _function, 
          previousDfa.getInputAlphabet(), _function_1);
        final CompactDFA<String> newDfa = NFAs.<String>determinize(newNfa, newNfa.getInputAlphabet(), true, false);
        final CompactDFA<String> newDfaNoTau = AutomataLibUtil.normalizeWeakTrace(newDfa);
        PostProcessingStatus _resultStatus = this.getResultStatus(componentModel.status);
        PostProcessingModelCompactDfa _postProcessingModelCompactDfa = new PostProcessingModelCompactDfa(newDfaNoTau, component, _resultStatus);
        models.put(component, _postProcessingModelCompactDfa);
      }
    }
  }
  
  public HideActions(final HideActionsOptions options) {
    super(options);
  }
}
