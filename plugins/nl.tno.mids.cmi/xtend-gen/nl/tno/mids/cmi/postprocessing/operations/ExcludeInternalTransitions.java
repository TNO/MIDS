package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Objects;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.NFAs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.GrowingAlphabet;
import net.automatalib.words.impl.Alphabets;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.MapExtensions;

/**
 * Exclude internal transitions (transitions which are not communicating) where possible.
 */
@Accessors
@SuppressWarnings("all")
public class ExcludeInternalTransitions extends PostProcessingOperation<ExcludeInternalTransitionsOptions> {
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
    monitor.subTask("Excluding internal transitions");
    final Function1<PostProcessingModel, Alphabet<String>> _function = (PostProcessingModel it) -> {
      return it.getCompactDfa().getInputAlphabet();
    };
    final Map<String, Alphabet<String>> componentAlphabets = MapExtensions.<String, PostProcessingModel, Alphabet<String>>mapValues(models, _function);
    final Consumer<String> _function_1 = (String component) -> {
      final PostProcessingModel componentModel = models.get(component);
      this.getPreconditionSubset().ensureSubset(componentModel);
      final CompactDFA<String> previousDfa = componentModel.getCompactDfa();
      final Function2<String, Alphabet<String>, Boolean> _function_2 = (String k, Alphabet<String> v) -> {
        return Boolean.valueOf((!Objects.equal(k, component)));
      };
      final Function<Alphabet<String>, Stream<String>> _function_3 = (Alphabet<String> a) -> {
        return a.stream();
      };
      final GrowingAlphabet<String> contextAlphabet = MapExtensions.<String, Alphabet<String>>filter(componentAlphabets, _function_2).values().stream().<String>flatMap(_function_3).collect(Alphabets.<String>collector());
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
      final Function<Alphabet<String>, CompactNFA<String>> _function_4 = (Alphabet<String> a) -> {
        return new CompactNFA<String>(a);
      };
      final Function<String, String> _function_5 = (String t) -> {
        String _xifexpression = null;
        if ((contextAlphabet.containsSymbol(t) || serviceFragmentEvents.contains(t))) {
          _xifexpression = t;
        } else {
          _xifexpression = "tau";
        }
        return _xifexpression;
      };
      final CompactNFA<String> newNfa = AutomataLibUtil.<Integer, Integer, String, String, CompactDFA<String>, CompactNFA<String>>rename(previousDfa, _function_4, 
        previousDfa.getInputAlphabet(), _function_5);
      final CompactDFA<String> newDfa = NFAs.<String>determinize(newNfa, newNfa.getInputAlphabet(), true, false);
      final CompactDFA<String> newDfaNoTau = AutomataLibUtil.normalizeWeakTrace(newDfa);
      PostProcessingStatus _resultStatus = this.getResultStatus(componentModel.status);
      PostProcessingModelCompactDfa _postProcessingModelCompactDfa = new PostProcessingModelCompactDfa(newDfaNoTau, component, _resultStatus);
      models.put(component, _postProcessingModelCompactDfa);
    };
    selectedComponents.forEach(_function_1);
  }
  
  public ExcludeInternalTransitions(final ExcludeInternalTransitionsOptions options) {
    super(options);
  }
}
