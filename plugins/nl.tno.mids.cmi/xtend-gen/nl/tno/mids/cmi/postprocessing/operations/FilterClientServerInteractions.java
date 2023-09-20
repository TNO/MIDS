package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import nl.tno.mids.cif.extensions.AutomatonExtensions;
import nl.tno.mids.cif.extensions.EdgeExtensions;
import nl.tno.mids.cmi.api.basic.CmiBasicComponentQueries;
import nl.tno.mids.cmi.api.basic.CmiBasicServiceFragmentQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralEventQueries;
import nl.tno.mids.cmi.api.general.CmiGeneralModifications;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.automata.Alphabet;
import org.eclipse.escet.cif.metamodel.cif.automata.Automaton;
import org.eclipse.escet.cif.metamodel.cif.automata.Edge;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.emf.EMFHelper;
import org.eclipse.xtend.lib.annotations.Accessors;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.MapExtensions;

/**
 * Filter models to keep only the interactions between two components (i.e. a client and server).
 */
@Accessors
@SuppressWarnings("all")
public class FilterClientServerInteractions extends PostProcessingOperation<FilterClientServerInteractionsOptions> {
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
    String _componentName1 = this.options.getComponentName1();
    String _plus = ("Filtering client/server interactions: " + _componentName1);
    String _plus_1 = (_plus + " and ");
    String _componentName2 = this.options.getComponentName2();
    String _plus_2 = (_plus_1 + _componentName2);
    monitor.subTask(_plus_2);
    final BiConsumer<String, PostProcessingModel> _function = (String component, PostProcessingModel model) -> {
      this.getPreconditionSubset().ensureSubset(model);
    };
    models.forEach(_function);
    final HashSet<String> componentsToRemove = CollectionLiterals.<String>newHashSet();
    final Function2<String, PostProcessingModel, Boolean> _function_1 = (String k, PostProcessingModel v) -> {
      return Boolean.valueOf(((!Objects.equal(k, this.options.getComponentName1())) && (!Objects.equal(k, this.options.getComponentName2()))));
    };
    final BiConsumer<String, PostProcessingModel> _function_2 = (String k, PostProcessingModel v) -> {
      componentsToRemove.add(k);
    };
    MapExtensions.<String, PostProcessingModel>filter(models, _function_1).forEach(_function_2);
    final Consumer<String> _function_3 = (String k) -> {
      models.remove(k);
    };
    componentsToRemove.forEach(_function_3);
    final PostProcessingModel component1 = models.get(this.options.getComponentName1());
    final PostProcessingModel component2 = models.get(this.options.getComponentName2());
    String _componentName1_1 = this.options.getComponentName1();
    String _plus_3 = ("Component not found: " + _componentName1_1);
    Preconditions.<PostProcessingModel>checkNotNull(component1, _plus_3);
    String _componentName2_1 = this.options.getComponentName2();
    String _plus_4 = ("Component not found: " + _componentName2_1);
    Preconditions.<PostProcessingModel>checkNotNull(component2, _plus_4);
    final Specification cifSpec1 = component1.getCifSpec();
    this.filterInteractions(cifSpec1, this.options.getComponentName2());
    final Specification cifSpec2 = component2.getCifSpec();
    this.filterInteractions(cifSpec2, this.options.getComponentName1());
    String _componentName1_2 = this.options.getComponentName1();
    PostProcessingStatus _resultStatus = this.getResultStatus(component1.status);
    PostProcessingModelCifSpec _postProcessingModelCifSpec = new PostProcessingModelCifSpec(cifSpec1, _componentName1_2, _resultStatus);
    models.put(this.options.getComponentName1(), _postProcessingModelCifSpec);
    String _componentName2_2 = this.options.getComponentName2();
    PostProcessingStatus _resultStatus_1 = this.getResultStatus(component2.status);
    PostProcessingModelCifSpec _postProcessingModelCifSpec_1 = new PostProcessingModelCifSpec(cifSpec2, _componentName2_2, _resultStatus_1);
    models.put(this.options.getComponentName2(), _postProcessingModelCifSpec_1);
  }
  
  private void filterInteractions(final Specification model, final String otherComponentName) {
    final Automaton automaton = CmiBasicComponentQueries.getSingleComponentWithBehavior(model);
    final EList<Edge> initialEdges = CmiBasicServiceFragmentQueries.getServiceFragmentInitialEdges(automaton);
    final HashSet<Edge> edgesToRemove = CollectionLiterals.<Edge>newHashSet();
    for (final Edge initialEdge : initialEdges) {
      {
        final Set<Edge> fragmentEdges = CmiBasicServiceFragmentQueries.getServiceFragmentEdges(automaton, initialEdge);
        final Function1<Edge, Boolean> _function = (Edge it) -> {
          return Boolean.valueOf(FilterClientServerInteractions.isCommunicationWith(it, otherComponentName));
        };
        boolean _exists = IterableExtensions.<Edge>exists(fragmentEdges, _function);
        boolean _not = (!_exists);
        if (_not) {
          edgesToRemove.add(initialEdge);
        }
      }
    }
    final Consumer<Edge> _function = (Edge it) -> {
      EMFHelper.removeFromParentContainment(it);
    };
    edgesToRemove.forEach(_function);
    AutomatonExtensions.removeUnreachableLocations(automaton);
    Alphabet _alphabet = automaton.getAlphabet();
    boolean _tripleNotEquals = (_alphabet != null);
    if (_tripleNotEquals) {
      AutomatonExtensions.updateAlphabet(automaton);
    }
    CmiGeneralModifications.removeUnusedEvents(model);
    CmiGeneralModifications.removeEmptyGroups(model);
  }
  
  private static boolean isCommunicationWith(final Edge edge, final String componentName) {
    final Event event = EdgeExtensions.getEventDecl(edge, false);
    final EventInfo eventInfo = CmiGeneralEventQueries.getEventInfo(event);
    return (Objects.equal(eventInfo.declCompInfo.toString(), componentName) || ((eventInfo.otherCompInfo != null) && Objects.equal(eventInfo.otherCompInfo.toString(), componentName)));
  }
  
  public FilterClientServerInteractions(final FilterClientServerInteractionsOptions options) {
    super(options);
  }
}
