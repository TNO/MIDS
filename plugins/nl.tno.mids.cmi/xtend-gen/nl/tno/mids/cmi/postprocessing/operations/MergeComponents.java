package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.automatalib.automata.fsa.impl.FastNFA;
import net.automatalib.automata.fsa.impl.FastNFAState;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.util.automata.fsa.NFAs;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.info.ComponentInfo;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCompactDfa;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.MapExtensions;

/**
 * Merge multiple runtime components, for instance multiple instances of the same executable, into a single runtime
 * component.
 */
@Accessors
@SuppressWarnings("all")
public class MergeComponents extends PostProcessingOperation<MergeComponentsOptions> {
  private Pattern pattern;
  
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
    final BiConsumer<String, PostProcessingModel> _function = (String component, PostProcessingModel model) -> {
      this.getPreconditionSubset().ensureSubset(model);
    };
    models.forEach(_function);
    final HashMap<ComponentInfo, ComponentInfo> componentRenameMap = CollectionLiterals.<ComponentInfo, ComponentInfo>newHashMap();
    final Function1<String, Boolean> _function_1 = (String key) -> {
      return Boolean.valueOf(this.getComponentNameMatcher(key).matches());
    };
    final Set<String> componentsToMerge = IterableExtensions.<String>toSet(IterableExtensions.<String>filter(selectedComponents, _function_1));
    while ((!componentsToMerge.isEmpty())) {
      {
        String _head = IterableExtensions.<String>head(componentsToMerge);
        final ComponentInfo componentInfo = new ComponentInfo(_head);
        final Matcher matcher = this.getComponentNameMatcher(componentInfo);
        final boolean matchResult = matcher.matches();
        String _head_1 = IterableExtensions.<String>head(componentsToMerge);
        String _plus = ("Expected match for " + _head_1);
        Preconditions.checkArgument(matchResult, _plus);
        String _group = matcher.group("name");
        final ComponentInfo baseInfo = new ComponentInfo(_group, null, componentInfo.traced);
        final Function1<String, Boolean> _function_2 = (String key) -> {
          return Boolean.valueOf(this.isComponentToMerge(key, baseInfo));
        };
        final Set<String> currentComponentsToMerge = IterableExtensions.<String>toSet(IterableExtensions.<String>filter(componentsToMerge, _function_2));
        final Consumer<String> _function_3 = (String key) -> {
          ComponentInfo _componentInfo = new ComponentInfo(key);
          componentRenameMap.put(_componentInfo, baseInfo);
        };
        currentComponentsToMerge.forEach(_function_3);
        final String head = IterableExtensions.<String>head(currentComponentsToMerge);
        final CompactDFA<String> firstDfa = models.get(head).getCompactDfa();
        final FastNFA<String> firstFastNfa = AutomataLibUtil.<String>dfaToNfa(AutomataLibUtil.<String>copy(firstDfa, firstDfa.getInputAlphabet()));
        final Function1<String, Boolean> _function_4 = (String k) -> {
          return Boolean.valueOf((!Objects.equal(k, head)));
        };
        final Iterable<String> otherComponentsToMerge = IterableExtensions.<String>filter(currentComponentsToMerge, _function_4);
        final Function2<String, PostProcessingModel, Boolean> _function_5 = (String k, PostProcessingModel v) -> {
          return Boolean.valueOf(IterableExtensions.contains(otherComponentsToMerge, k));
        };
        final Map<String, PostProcessingModel> otherComponentsModels = MapExtensions.<String, PostProcessingModel>filter(models, _function_5);
        final Function2<FastNFA<String>, PostProcessingModel, FastNFA<String>> _function_6 = (FastNFA<String> left, PostProcessingModel right) -> {
          return this.mergeInitialStates(left, right.getCompactDfa());
        };
        final FastNFA<String> resultNfa = IterableExtensions.<PostProcessingModel, FastNFA<String>>fold(otherComponentsModels.values(), firstFastNfa, _function_6);
        final CompactDFA<String> resultDfa = NFAs.<String>determinize(resultNfa, resultNfa.getInputAlphabet(), true, false);
        final CompactDFA<String> minimizedResultDfa = AutomataLibUtil.<String>minimizeDFA(resultDfa);
        final Consumer<String> _function_7 = (String key) -> {
          componentsToMerge.remove(key);
          models.remove(key);
        };
        currentComponentsToMerge.forEach(_function_7);
        boolean _containsKey = models.containsKey(baseInfo.toString());
        boolean _not = (!_containsKey);
        String _string = baseInfo.toString();
        String _plus_1 = (_string + " already exists in model set");
        Preconditions.checkArgument(_not, _plus_1);
        String _string_1 = baseInfo.toString();
        String _string_2 = baseInfo.toString();
        PostProcessingStatus _postProcessingStatus = new PostProcessingStatus(false, false);
        PostProcessingModelCompactDfa _postProcessingModelCompactDfa = new PostProcessingModelCompactDfa(minimizedResultDfa, _string_2, _postProcessingStatus);
        models.put(_string_1, _postProcessingModelCompactDfa);
      }
    }
    final Consumer<Map.Entry<String, PostProcessingModel>> _function_2 = (Map.Entry<String, PostProcessingModel> entry) -> {
      final CompactNFA<String> nfa = AutomataLibUtil.<String>dfaToNfa(entry.getValue().getCompactDfa());
      final Function<String, String> _function_3 = (String event) -> {
        return this.normalizeMergedComponentNamesInEvent(event, componentRenameMap);
      };
      final CompactNFA<String> renamedNfa = AutomataLibUtil.<String, String>rename(nfa, _function_3);
      final CompactDFA<String> renamedDfa = NFAs.<String>determinize(renamedNfa, renamedNfa.getInputAlphabet(), true, false);
      final CompactDFA<String> minimizedRenamedDfa = AutomataLibUtil.<String>minimizeDFA(renamedDfa);
      final Specification renamedCif = AutomataLibToCif.<Integer, Integer, Void, CompactDFA<String>>fsaToCifSpecification(minimizedRenamedDfa, entry.getValue().name, true);
      final Automaton automaton = CmiBasicComponentQueries.getSingleComponentWithBehavior(renamedCif);
      AutomatonExtensions.ensureInitialLocationIsFirstLocation(automaton);
      PostProcessingStatus _resultStatus = this.getResultStatus(entry.getValue().status);
      PostProcessingModelCifSpec _postProcessingModelCifSpec = new PostProcessingModelCifSpec(renamedCif, entry.getValue().name, _resultStatus);
      entry.setValue(_postProcessingModelCifSpec);
    };
    models.entrySet().forEach(_function_2);
  }
  
  private String normalizeMergedComponentNamesInEvent(final String eventName, final Map<ComponentInfo, ComponentInfo> componentRenameMap) {
    EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(eventName);
    boolean _containsKey = componentRenameMap.containsKey(eventInfo.declCompInfo);
    if (_containsKey) {
      eventInfo = eventInfo.withDeclCompInfo(componentRenameMap.get(eventInfo.declCompInfo));
    }
    if ((eventInfo.otherCompInfo != null)) {
      boolean _containsKey_1 = componentRenameMap.containsKey(eventInfo.otherCompInfo);
      if (_containsKey_1) {
        eventInfo = eventInfo.withOtherCompInfo(componentRenameMap.get(eventInfo.otherCompInfo));
      }
    }
    return eventInfo.toString();
  }
  
  private Matcher getComponentNameMatcher(final String componentName) {
    ComponentInfo _componentInfo = new ComponentInfo(componentName);
    return this.getComponentNameMatcher(_componentInfo);
  }
  
  private Matcher getComponentNameMatcher(final ComponentInfo componentInfo) {
    return this.getPattern().matcher(componentInfo.name);
  }
  
  private Pattern getPattern() {
    if ((this.pattern == null)) {
      this.pattern = Pattern.compile(this.options.pattern);
    }
    return this.pattern;
  }
  
  private boolean isComponentToMerge(final String componentName, final ComponentInfo baseInfo) {
    final ComponentInfo componentInfo = new ComponentInfo(componentName);
    final Matcher matcher = this.getComponentNameMatcher(componentInfo);
    boolean _matches = matcher.matches();
    if (_matches) {
      return (Objects.equal(matcher.group("name"), baseInfo.name) && (componentInfo.traced == baseInfo.traced));
    } else {
      return false;
    }
  }
  
  private FastNFA<String> mergeInitialStates(final FastNFA<String> left, final CompactDFA<String> right) {
    int _size = left.getInitialStates().size();
    boolean _equals = (_size == 1);
    Preconditions.checkArgument(_equals);
    boolean _isAccepting = left.isAccepting(IterableExtensions.<FastNFAState>head(left.getInitialStates()));
    boolean _isAccepting_1 = right.isAccepting(right.getInitialState());
    boolean _equals_1 = (_isAccepting == _isAccepting_1);
    Preconditions.checkArgument(_equals_1);
    final Consumer<String> _function = (String it) -> {
      left.addAlphabetSymbol(it);
    };
    right.getInputAlphabet().forEach(_function);
    final HashMap<Integer, FastNFAState> stateMap = CollectionLiterals.<Integer, FastNFAState>newHashMap();
    stateMap.put(right.getInitialState(), IterableExtensions.<FastNFAState>head(left.getInitialStates()));
    final Function1<Integer, Boolean> _function_1 = (Integer it) -> {
      Integer _initialState = right.getInitialState();
      return Boolean.valueOf((!Objects.equal(it, _initialState)));
    };
    final Consumer<Integer> _function_2 = (Integer it) -> {
      final FastNFAState newState = left.addState(right.isAccepting(it));
      stateMap.put(it, newState);
    };
    IterableExtensions.<Integer>filter(right.getStates(), _function_1).forEach(_function_2);
    final Consumer<Integer> _function_3 = (Integer state) -> {
      final Collection<String> localInputs = right.getLocalInputs(state);
      final Consumer<String> _function_4 = (String input) -> {
        final Integer nextState = right.getSuccessor(state, input);
        left.addTransition(stateMap.get(state), input, stateMap.get(nextState));
      };
      localInputs.forEach(_function_4);
    };
    right.getStates().forEach(_function_3);
    return left;
  }
  
  public MergeComponents(final MergeComponentsOptions options) {
    super(options);
  }
  
  public void setPattern(final Pattern pattern) {
    this.pattern = pattern;
  }
}
