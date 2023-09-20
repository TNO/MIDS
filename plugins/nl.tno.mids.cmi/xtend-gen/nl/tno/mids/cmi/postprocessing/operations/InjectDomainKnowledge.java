package nl.tno.mids.cmi.postprocessing.operations;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.FileExtensions;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Exceptions;

@Accessors
@SuppressWarnings("all")
public class InjectDomainKnowledge extends PostProcessingOperation<InjectDomainKnowledgeOptions> {
  @Override
  public PostProcessingPreconditionSubset getPreconditionSubset() {
    return new PostProcessingPreconditionSubset(Boolean.valueOf(false), Boolean.valueOf(false));
  }
  
  @Override
  public PostProcessingResultSubset getResultSubset() {
    return new PostProcessingResultSubset(Boolean.valueOf(false), Boolean.valueOf(false));
  }
  
  @Override
  public void applyOperation(final Map<String, PostProcessingModel> models, final Set<String> selectedComponents, final Path relativeResolvePath, final IProgressMonitor monitor) {
    final SubMonitor subMonitor = SubMonitor.convert(monitor, selectedComponents.size());
    for (final String component : selectedComponents) {
      {
        final PostProcessingModel previousModel = models.get(component);
        this.getPreconditionSubset().ensureSubset(previousModel);
        final CompactDFA<String> newDfa = InjectDomainKnowledge.injectDomainKnowledge(previousModel.getCompactDfa(), relativeResolvePath, this.options, 
          subMonitor.split(1));
        if ((newDfa != null)) {
          PostProcessingStatus _resultStatus = this.getResultStatus(previousModel.status);
          PostProcessingModelCompactDfa _postProcessingModelCompactDfa = new PostProcessingModelCompactDfa(newDfa, component, _resultStatus);
          models.put(component, _postProcessingModelCompactDfa);
        }
      }
    }
  }
  
  /**
   * Apply domain knowledge injection.
   * 
   * @param previousDfa The DFA on which to inject the domain knowledge.
   * @param relativeResolvePath The absolute path of the input file. During post-processing, all relative paths are
   *      to be resolved against the directory that contains this file.
   * @param options The domain knowledge injection options.
   * @param monitor The progress monitor.
   * @return The DFA resulting from injection, or {@code null} if domain knowledge injection did not apply.
   */
  private static CompactDFA<String> injectDomainKnowledge(final CompactDFA<String> previousDfa, final Path relativeResolvePath, final InjectDomainKnowledgeOptions options, final IProgressMonitor monitor) {
    try {
      String _string = options.toString();
      String _plus = ("Injecting domain knowledge: " + _string);
      final SubMonitor subMonitor = SubMonitor.convert(monitor, _plus, 71);
      subMonitor.split(10);
      final Path absOrRelInjectPath = Paths.get(options.modelPath);
      final Path absInjectResolvePath = relativeResolvePath.getParent();
      final Path absInjectPath = absInjectResolvePath.resolve(absOrRelInjectPath).normalize();
      Specification injectSpec = FileExtensions.loadCIF(absInjectPath);
      subMonitor.split(10);
      PostProcessingStatus _postProcessingStatus = new PostProcessingStatus(true, true);
      final PostProcessingModelCifSpec injectionModel = new PostProcessingModelCifSpec(injectSpec, "<inject>", _postProcessingStatus);
      final PostProcessingModel ensuredInjectionModel = new PostProcessingPreconditionSubset(Boolean.valueOf(false), Boolean.valueOf(false)).ensureSubset(injectionModel);
      final CompactDFA<String> injectDfa = ensuredInjectionModel.getCompactDfa();
      subMonitor.split(1);
      final Predicate<Integer> _function = (Integer it) -> {
        boolean _isAccepting = injectDfa.isAccepting(it);
        return (!_isAccepting);
      };
      boolean _allMatch = injectDfa.getStates().stream().allMatch(_function);
      if (_allMatch) {
        throw new RuntimeException((("Domain knowledge model \"" + options.modelPath) + 
          "\" does not have any accepting states. Ensure the CIF model contains appropriate marking."));
      }
      subMonitor.split(50);
      CompactDFA<String> _switchResult = null;
      final InjectDomainKnowledgeOperator _switchValue = options.operator;
      if (_switchValue != null) {
        switch (_switchValue) {
          case DIFFERENCE_LEFT:
            _switchResult = AutomataLibUtil.<Integer, String, CompactDFA<String>>differenceMinimized(injectDfa, previousDfa);
            break;
          case DIFFERENCE_RIGHT:
            _switchResult = AutomataLibUtil.<Integer, String, CompactDFA<String>>differenceMinimized(previousDfa, injectDfa);
            break;
          case EXCLUSIVE_OR:
            _switchResult = AutomataLibUtil.<Integer, String, CompactDFA<String>>xorMinimized(previousDfa, injectDfa);
            break;
          case INTERSECTION:
            _switchResult = AutomataLibUtil.<Integer, String, CompactDFA<String>>intersectionMinimized(previousDfa, injectDfa);
            break;
          case PARALLEL_COMPOSITION:
            _switchResult = AutomataLibUtil.<Integer, String, CompactDFA<String>>parallelCompositionMinimized(previousDfa, injectDfa);
            break;
          case UNION:
            _switchResult = AutomataLibUtil.<Integer, String, CompactDFA<String>>unionMinimized(previousDfa, injectDfa);
            break;
          default:
            throw new RuntimeException(("Unknown operator: " + options.operator));
        }
      } else {
        throw new RuntimeException(("Unknown operator: " + options.operator));
      }
      final CompactDFA<String> newDfa = _switchResult;
      return newDfa;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public InjectDomainKnowledge(final InjectDomainKnowledgeOptions options) {
    super(options);
  }
}
