package nl.tno.mids.cmi;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.automatalib.automata.ShrinkableAutomaton;
import net.automatalib.automata.concepts.InputAlphabetHolder;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.FastDFA;
import net.automatalib.automata.fsa.impl.FastDFAState;
import net.automatalib.words.Word;
import net.automatalib.words.WordBuilder;
import nl.esi.pps.architecture.instantiated.Executor;
import nl.esi.pps.tmsc.Event;
import nl.esi.pps.tmsc.ExitEvent;
import nl.esi.pps.tmsc.Lifeline;
import nl.esi.pps.tmsc.TMSC;
import nl.esi.pps.tmsc.compare.ArchitectureLifecycleStage;
import nl.esi.pps.tmsc.text.ETimestampFormat;
import nl.tno.mids.automatalib.extensions.cif.AutomataLibToCif;
import nl.tno.mids.automatalib.extensions.util.AutomataLibUtil;
import nl.tno.mids.automatalib.extensions.util.IncrementalMutableDFATreeBuilder;
import nl.tno.mids.cmi.utils.CifNamesUtil;
import nl.tno.mids.pps.extensions.queries.TmscEventQueries;
import nl.tno.mids.pps.extensions.queries.TmscExecutionQueries;
import nl.tno.mids.pps.extensions.queries.TmscLifelineQueries;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.Pure;

@SuppressWarnings("all")
public class ComponentModelBuilder {
  private final LinkedHashMap<String, IncrementalMutableDFATreeBuilder<String>> componentAutomata;
  
  @Accessors
  private boolean synchronous;
  
  public ComponentModelBuilder() {
    this(false);
  }
  
  public ComponentModelBuilder(final boolean synchronous) {
    LinkedHashMap<String, IncrementalMutableDFATreeBuilder<String>> _linkedHashMap = new LinkedHashMap<String, IncrementalMutableDFATreeBuilder<String>>();
    this.componentAutomata = _linkedHashMap;
    this.synchronous = synchronous;
  }
  
  /**
   * Builds and inserts models for all execution call stacks that are in scope of {@code tmsc}.
   * 
   * @param tmsc The {@link TMSC} that determines the scope for model construction.
   */
  public void insert(final TMSC tmsc) {
    final Consumer<Lifeline> _function = (Lifeline it) -> {
      this.insert(it, tmsc);
    };
    TmscLifelineQueries.nonEmptyLifelinesOf(tmsc).forEach(_function);
  }
  
  /**
   * Builds and inserts models for all execution call stacks on {@code lifeline} that are in scope of {@code tmsc}.
   * 
   * @param lifeline The {@link Lifeline} from which models are to be constructed.
   * @param tmsc The {@link TMSC} that determines the scope for model construction.
   */
  public void insert(final Lifeline lifeline, final TMSC tmsc) {
    final Function1<Event, Boolean> _function = (Event it) -> {
      return Boolean.valueOf(TmscEventQueries.isInScope(tmsc, it));
    };
    final Function1<Event, Long> _function_1 = (Event it) -> {
      return it.getTimestamp();
    };
    final Iterator<Event> iterator = IterableExtensions.<Event, Long>sortBy(IterableExtensions.<Event>filter(lifeline.getEvents(), _function), _function_1).iterator();
    final IncrementalMutableDFATreeBuilder<String> builder = this.getBuilder(lifeline);
    Event event = null;
    try {
      while (iterator.hasNext()) {
        {
          final WordBuilder<String> wordBuilder = new WordBuilder<String>();
          do {
            {
              event = iterator.next();
              wordBuilder.append(CifNamesUtil.asCifName(event, tmsc, this.synchronous));
            }
          } while((!this.isRootExitEventInScope(event, tmsc)));
          builder.insert(wordBuilder.toWord());
        }
      }
    } catch (final Throwable _t) {
      if (_t instanceof NoSuchElementException) {
        final NoSuchElementException cause = (NoSuchElementException)_t;
        String _describe = ArchitectureLifecycleStage.INSTANTIATED.describe(event, false);
        String _plus = ("Invalid stack at event " + _describe);
        String _plus_1 = (_plus + " @ ");
        String _format = ETimestampFormat.eINSTANCE.format(event.getTimestamp());
        String _plus_2 = (_plus_1 + _format);
        throw new RuntimeException(_plus_2, cause);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  private boolean isRootExitEventInScope(final Event event, final TMSC tmsc) {
    return ((event instanceof ExitEvent) && (TmscExecutionQueries.getRootInScope(tmsc, event.getExecution()) == null));
  }
  
  public void insert(final String componentLabel, final Word<String> word) {
    final IncrementalMutableDFATreeBuilder<String> builder = this.getBuilder(componentLabel);
    builder.insert(word);
  }
  
  public Set<String> getComponentLabels() {
    return this.componentAutomata.keySet();
  }
  
  public FastDFA<String> getPTA(final Executor executor) {
    return this.getPTA(CifNamesUtil.asCifName(executor));
  }
  
  public FastDFA<String> getPTA(final String componentLabel) {
    return this.componentAutomata.get(componentLabel).getFastDFA(true);
  }
  
  public LinkedHashMap<String, FastDFA<String>> getPTAs() {
    int _size = this.componentAutomata.size();
    final LinkedHashMap<String, FastDFA<String>> ptas = new LinkedHashMap<String, FastDFA<String>>(_size);
    final BiConsumer<String, IncrementalMutableDFATreeBuilder<String>> _function = (String k, IncrementalMutableDFATreeBuilder<String> v) -> {
      ptas.put(k, this.getPTA(k));
    };
    this.componentAutomata.forEach(_function);
    return ptas;
  }
  
  public FastDFA<String> getDFA(final Executor executor) {
    return this.getDFA(CifNamesUtil.asCifName(executor));
  }
  
  public FastDFA<String> getDFA(final String componentLabel) {
    final FastDFA<String> pta = this.getPTA(componentLabel);
    final FastDFA<String> dfa = AutomataLibUtil.<String>minimizeDFA(pta);
    ComponentModelBuilder.<String, FastDFAState, FastDFA<String>>loop(dfa);
    return dfa;
  }
  
  public LinkedHashMap<String, FastDFA<String>> getDFAs() {
    int _size = this.componentAutomata.size();
    final LinkedHashMap<String, FastDFA<String>> dfas = new LinkedHashMap<String, FastDFA<String>>(_size);
    final BiConsumer<String, IncrementalMutableDFATreeBuilder<String>> _function = (String k, IncrementalMutableDFATreeBuilder<String> v) -> {
      dfas.put(k, this.getDFA(k));
    };
    this.componentAutomata.forEach(_function);
    return dfas;
  }
  
  /**
   * Returns a CIF specification with the all the models learned by this learner
   */
  public Specification getCifComposition() {
    return this.getCifComposition(this.getDFAs());
  }
  
  /**
   * Returns a CIF specification with the specified automata. Allows use of this.getCifComposition(this.getPTAs())
   * to obtain PTA composition.
   */
  public Specification getCifComposition(final Map<String, FastDFA<String>> automata) {
    return AutomataLibToCif.<FastDFAState, FastDFAState, Void, FastDFA<String>>fsasToCifSpecification(automata, true);
  }
  
  /**
   * Returns a map from string to specification, containing a CIF specification for each model learned by this learner
   */
  public LinkedHashMap<String, Specification> getCifModels() {
    return this.getCifModels(this.getDFAs());
  }
  
  /**
   * Returns a map from string to specification, containing a CIF specification for each model passed.
   */
  public LinkedHashMap<String, Specification> getCifModels(final Map<String, FastDFA<String>> automata) {
    int _size = automata.size();
    final LinkedHashMap<String, Specification> ret = new LinkedHashMap<String, Specification>(_size);
    final BiConsumer<String, FastDFA<String>> _function = (String name, FastDFA<String> dfa) -> {
      ret.put(name, this.getCifModel(name, dfa));
    };
    automata.forEach(_function);
    return ret;
  }
  
  /**
   * Returns a CIF specification for the model passed.
   */
  public Specification getCifModel(final String name, final FastDFA<String> dfa) {
    return AutomataLibToCif.<FastDFAState, FastDFAState, Void, FastDFA<String>>fsaToCifSpecification(dfa, name, true);
  }
  
  protected IncrementalMutableDFATreeBuilder<String> getBuilder(final Lifeline lifeline) {
    return this.getBuilder(CifNamesUtil.asCifName(lifeline.getExecutor()));
  }
  
  protected IncrementalMutableDFATreeBuilder<String> getBuilder(final String componentLabel) {
    boolean _containsKey = this.componentAutomata.containsKey(componentLabel);
    if (_containsKey) {
      return this.componentAutomata.get(componentLabel);
    } else {
      final IncrementalMutableDFATreeBuilder<String> builder = new IncrementalMutableDFATreeBuilder<String>();
      this.componentAutomata.put(componentLabel, builder);
      return builder;
    }
  }
  
  protected static <I extends Object, S extends Object, A extends ShrinkableAutomaton<S, I, S, Boolean, Void> & DFA<S, I> & InputAlphabetHolder<I>> A loop(final A dfa) {
    int _size = dfa.getInitialStates().size();
    boolean _equals = (_size == 1);
    Preconditions.checkState(_equals);
    final Function1<S, Boolean> _function = (S it) -> {
      return dfa.getStateProperty(it);
    };
    List<S> acceptingStates = IterableExtensions.<S>toList(IterableExtensions.<S>filter(dfa.getStates(), _function));
    int _size_1 = acceptingStates.size();
    boolean _equals_1 = (_size_1 == 1);
    Preconditions.checkState(_equals_1);
    S acceptingState = acceptingStates.get(0);
    int _size_2 = dfa.size();
    boolean _greaterThan = (_size_2 > 1);
    if (_greaterThan) {
      boolean _equals_2 = acceptingState.equals(dfa.getInitialState());
      boolean _not = (!_equals_2);
      Preconditions.checkState(_not);
    }
    AutomataLibUtil.<I, S, S, Boolean, Void, A>merge(dfa, dfa.getInitialState(), acceptingState, true);
    dfa.setStateProperty(dfa.getInitialState(), Boolean.valueOf(true));
    return dfa;
  }
  
  @Pure
  public boolean isSynchronous() {
    return this.synchronous;
  }
  
  public void setSynchronous(final boolean synchronous) {
    this.synchronous = synchronous;
  }
}
