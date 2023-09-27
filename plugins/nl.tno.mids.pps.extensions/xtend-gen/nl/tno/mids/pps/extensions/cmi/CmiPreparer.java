package nl.tno.mids.pps.extensions.cmi;

import com.google.common.collect.Iterables;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import nl.esi.emf.properties.PropertiesContainer;
import nl.esi.pps.architecture.instantiated.Executor;
import nl.esi.pps.tmsc.Dependency;
import nl.esi.pps.tmsc.Event;
import nl.esi.pps.tmsc.FullScopeTMSC;
import nl.esi.pps.tmsc.Lifeline;
import nl.esi.pps.tmsc.LifelineSegment;
import nl.esi.pps.tmsc.ScopedTMSC;
import nl.esi.pps.tmsc.TMSC;
import nl.esi.pps.tmsc.TmscFactory;
import nl.esi.pps.tmsc.util.TmscQueries;
import nl.tno.mids.pps.extensions.info.EventFunctionExecutionType;
import nl.tno.mids.pps.extensions.queries.TmscLifelineQueries;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.emf.common.util.EList;
import org.eclipse.lsat.common.util.PairwiseIterable;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;

/**
 * A transformation for preparing {@link TMSC TMSCs} for CMI. This transformation determines and annotates all
 * information that is essential for performing CMI.
 */
@SuppressWarnings("all")
public abstract class CmiPreparer {
  @Extension
  private static final TmscFactory m_tmsc = TmscFactory.eINSTANCE;
  
  public abstract boolean appliesTo(final Dependency dependency);
  
  /**
   * Transforms the given {@code tmsc} by adding a {@link ScopedTMSC scope} to it named {@code scopeName} that
   * contains all {@link Event events} and {@link Dependency dependencies} that are to be considered by CMI. Moreover,
   * this new {@link ScopedTMSC scope} is prepared in the sense that essential information required by CMI is
   * determined and annotated by means of properties (see also {@link PropertiesContainer}).
   * 
   * @param tmsc The {@link TMSC} to be scoped.
   * @param scopeName The name of the {@link ScopedTMSC scope} to create.
   * @param warnings The warnings produced during the operation.
   * @param tmscPath The path to TMSC file, that can be used to find additional files. May be {@code null} if TMSC is
   *      not loaded from a file, in which case no additional files can be located.
   * @return The CMI {@link ScopedTMSC scope} that has been added to {@code tmsc}.
   */
  public ScopedTMSC prepare(final FullScopeTMSC tmsc, final String scopeName, final List<String> warnings, final Path tmscPath) {
    ScopedTMSC _xblockexpression = null;
    {
      final Function1<ScopedTMSC, Boolean> _function = (ScopedTMSC it) -> {
        return Boolean.valueOf(it.getName().equals(scopeName));
      };
      boolean _exists = IterableExtensions.<ScopedTMSC>exists(tmsc.getFullScope().getChildScopes(), _function);
      if (_exists) {
        warnings.add(("Removed existing scope named " + scopeName));
      }
      final Predicate<ScopedTMSC> _function_1 = (ScopedTMSC it) -> {
        return it.getName().equals(scopeName);
      };
      tmsc.getFullScope().getChildScopes().removeIf(_function_1);
      final ScopedTMSC scopedTmsc = this.scope(tmsc, scopeName);
      List<Lifeline> _nonEmptyLifelinesOf = TmscLifelineQueries.nonEmptyLifelinesOf(scopedTmsc);
      for (final Lifeline lifeline : _nonEmptyLifelinesOf) {
        Executor _executor = lifeline.getExecutor();
        CmiPreparer.setComponentName(_executor, this.componentNameFor(lifeline));
      }
      Collection<Event> _events = scopedTmsc.getEvents();
      for (final Event event : _events) {
        {
          CmiPreparer.setFunctionName(event, this.functionNameFor(event));
          CmiPreparer.setInterfaceName(event, this.interfaceNameFor(event));
          CmiPreparer.setExecutionType(event, this.executionTypeFor(event));
        }
      }
      _xblockexpression = scopedTmsc;
    }
    return _xblockexpression;
  }
  
  /**
   * Creates a {@link ScopedTMSC scoped TMSC} named {@code scopeName} out of {@code tmsc} that contains only the
   * {@link Event events} and {@link Dependency dependencies} that are relevant for CMI, according to this preparer.
   * <p>
   * Any implementer of this method can assume that {@code scopeName} is a free scope name in {@code tmsc}.
   * </p>
   * 
   * @param tmsc The {@link TMSC} to scope.
   * @param scopeName The name of the {@link ScopedTMSC scoped TMSC} to create.
   * @return The {@link ScopedTMSC scoped TMSC} containing only relevant information for CMI.
   */
  protected abstract ScopedTMSC scope(final FullScopeTMSC tmsc, final String scopeName);
  
  /**
   * Determines a component name for {@code lifeline}.
   * 
   * @param lifeline The {@link Lifeline} for which a component name is to be determined.
   * @return The component name for {@code lifeline}, as a {@link String}.
   */
  protected abstract String componentNameFor(final Lifeline lifeline);
  
  /**
   * Determines a function name for {@code event}.
   * 
   * @param event The {@link Event} for which a function name is to be determined.
   * @return The name of the function that is executed by the specified {@code event}, as a {@link String}.
   */
  protected abstract String functionNameFor(final Event event);
  
  /**
   * Determines an interface name for {@code event}.
   * 
   * @param event The {@link Event} for which an interface name is to be determined.
   * @return The interface name of the function that is executed in the given {@code event}, as a {@link String}.
   */
  protected abstract String interfaceNameFor(final Event event);
  
  /**
   * Determines the function execution type of {@code event}.
   * 
   * @param event The {@link Event} for which a function execution type is to be determined.
   * @return The execution type of the function of the given {@code event}, as a {@link String}.
   */
  protected abstract EventFunctionExecutionType executionTypeFor(final Event event);
  
  /**
   * Helper method for creating a {@link ScopedTMSC scoped TMSC} named {@code scopeName} out of {@code tmsc} that
   * contains all {@link Dependency dependencies} of {@code tmsc} between the events that satisfy {@code predicate}.
   * 
   * @param tmsc The {@link FullScopeTMSC} to scope.
   * @param scopeName The name of the {@link ScopedTMSC scoped TMSC} to create.
   * @param predicate The {@link Predicate predicate} that determines which {@link Event events} are to be
   *                  included in the scope to create.
   * @return The {@link ScopedTMSC scoped TMSC} containing all {@link Dependency dependencies} between the events
   *         that satisfy {@code predicate}, which has also been added to the scopes of {@code tmsc}.
   */
  protected final ScopedTMSC scopeOnEvents(final FullScopeTMSC tmsc, final String scopeName, final Predicate<? super Event> predicate) {
    final Function1<Dependency, Boolean> _function = (Dependency it) -> {
      return Boolean.valueOf((predicate.test(it.getSource()) && predicate.test(it.getTarget())));
    };
    final Map<Boolean, List<Dependency>> scopeDependencies = IterableExtensions.<Boolean, Dependency>groupBy(tmsc.getDependencies(), _function);
    final ScopedTMSC scopedTmsc = TmscQueries.createScopedTMSC(scopeDependencies.get(Boolean.valueOf(true)), scopeName);
    EList<ScopedTMSC> _childScopes = tmsc.getChildScopes();
    _childScopes.add(scopedTmsc);
    boolean _containsKey = scopeDependencies.containsKey(Boolean.valueOf(false));
    boolean _not = (!_containsKey);
    if (_not) {
      return scopedTmsc;
    }
    final Function1<Lifeline, Iterable<LifelineSegment>> _function_1 = (Lifeline it) -> {
      return this.refineWithCompleteOrder(it, predicate);
    };
    final Function1<LifelineSegment, Boolean> _function_2 = (LifelineSegment it) -> {
      return Boolean.valueOf(it.isProjection());
    };
    final List<LifelineSegment> projections = IterableExtensions.<LifelineSegment>toList(IterableExtensions.<LifelineSegment>filter(IterableExtensions.<Lifeline, LifelineSegment>flatMap(tmsc.getLifelines(), _function_1), _function_2));
    EList<Dependency> _dependencies = scopedTmsc.getDependencies();
    Iterables.<Dependency>addAll(_dependencies, projections);
    EList<Dependency> _dependencies_1 = tmsc.getDependencies();
    Iterables.<Dependency>addAll(_dependencies_1, projections);
    return scopedTmsc;
  }
  
  private Iterable<LifelineSegment> refineWithCompleteOrder(final Lifeline lifeline, final Predicate<? super Event> predicate) {
    final Function1<Event, Boolean> _function = (Event it) -> {
      return Boolean.valueOf(predicate.test(it));
    };
    final Function1<Pair<Event, Event>, LifelineSegment> _function_1 = (Pair<Event, Event> it) -> {
      return this.findOrCreateLifelineSegement(it);
    };
    return IterableExtensions.<Pair<Event, Event>, LifelineSegment>map(PairwiseIterable.<Event>of(IterableExtensions.<Event>filter(lifeline.getEvents(), _function)), _function_1);
  }
  
  private LifelineSegment findOrCreateLifelineSegement(final Pair<Event, Event> eventPair) {
    final Function1<LifelineSegment, Boolean> _function = (LifelineSegment it) -> {
      Event _target = it.getTarget();
      Event _right = eventPair.getRight();
      return Boolean.valueOf((_target == _right));
    };
    final LifelineSegment lifelineSegment = IterableExtensions.<LifelineSegment>findFirst(Iterables.<LifelineSegment>filter(eventPair.getLeft().getFullScopeOutgoingDependencies(), LifelineSegment.class), _function);
    LifelineSegment _elvis = null;
    if (lifelineSegment != null) {
      _elvis = lifelineSegment;
    } else {
      LifelineSegment _createLifelineSegment = CmiPreparer.m_tmsc.createLifelineSegment();
      _elvis = _createLifelineSegment;
    }
    final Procedure1<LifelineSegment> _function_1 = (LifelineSegment it) -> {
      it.setSource(eventPair.getLeft());
      it.setTarget(eventPair.getRight());
      it.setProjection(true);
    };
    return ObjectExtensions.<LifelineSegment>operator_doubleArrow(_elvis, _function_1);
  }
  
  private static String getComponentName(final Executor container) {
    final String key = "componentName";
    final Object value = container.getProperties().get(key);
    return (String) value;
  }
  
  private static void setComponentName(final Executor container, final String value) {
    final String key = "componentName";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
  
  private static String getFunctionName(final Event container) {
    final String key = "functionName";
    final Object value = container.getProperties().get(key);
    return (String) value;
  }
  
  private static void setFunctionName(final Event container, final String value) {
    final String key = "functionName";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
  
  private static String getInterfaceName(final Event container) {
    final String key = "interfaceName";
    final Object value = container.getProperties().get(key);
    return (String) value;
  }
  
  private static void setInterfaceName(final Event container, final String value) {
    final String key = "interfaceName";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
  
  private static EventFunctionExecutionType getExecutionType(final Event container) {
    final String key = "executionType";
    final Object value = container.getProperties().get(key);
    return (EventFunctionExecutionType) value;
  }
  
  private static void setExecutionType(final Event container, final EventFunctionExecutionType value) {
    final String key = "executionType";
    if (value == null) {
    container.getProperties().remove(key);
    } else {
        container.getProperties().put(key, value);
    }
  }
}
