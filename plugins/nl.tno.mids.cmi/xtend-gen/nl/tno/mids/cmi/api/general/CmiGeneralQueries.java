package nl.tno.mids.cmi.api.general;

import java.util.HashSet;
import java.util.List;
import nl.tno.mids.cmi.api.basic.CmiBasicServiceFragmentQueries;
import nl.tno.mids.cmi.api.info.EventInfo;
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries;
import nl.tno.mids.cmi.api.split.CmiSplitServiceFragmentQueries;
import org.eclipse.escet.cif.common.CifScopeUtils;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.cif.metamodel.cif.declarations.Event;
import org.eclipse.escet.common.java.Assert;
import org.eclipse.escet.common.position.metamodel.position.PositionObject;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class CmiGeneralQueries {
  /**
   * Get the model in which the given object is found. If the given object is a model, it is itself returned.
   * 
   * @return The model.
   */
  public static Specification getModel(final PositionObject object) {
    return CifScopeUtils.getSpecRoot(CifScopeUtils.getScope(object));
  }
  
  /**
   * Does the given model use synchronous component composition (naming)?
   * 
   * @param model The model.
   * @return {@code true} if the model uses synchronous component composition (naming), {@code false} if
   *      it uses asynchronous component composition (naming).
   */
  public static Boolean usesSynchronousComposition(final Specification model) {
    Boolean synchronous = null;
    final List<Event> events = CmiGeneralEventQueries.getEvents(model);
    boolean _isEmpty = events.isEmpty();
    boolean _not = (!_isEmpty);
    Assert.check(_not, 
      "Can\'t determine whether model uses synchronous composition: model has no events.");
    for (final Event event : events) {
      {
        final EventInfo info = CmiGeneralEventQueries.getEventInfo(event);
        if ((info.asyncDirection != null)) {
          Assert.check(((synchronous).booleanValue() != true));
          synchronous = Boolean.valueOf(false);
        } else {
          Assert.check(((synchronous).booleanValue() != false));
          synchronous = Boolean.valueOf(true);
        }
      }
    }
    return synchronous;
  }
  
  /**
   * Detects the subset of the given model.
   * 
   * @param model The model.
   * @return The subset.
   */
  public static CmiSubset detectSubset(final Specification model) {
    final HashSet<CmiSubset> possibleSubsets = CollectionLiterals.<CmiSubset>newHashSet();
    boolean _isProtocolCmiModel = CmiProtocolQueries.isProtocolCmiModel(model);
    if (_isProtocolCmiModel) {
      possibleSubsets.add(CmiSubset.PROTOCOL);
    }
    boolean _isBasicCmiModelWithNoSplitServiceFragments = CmiBasicServiceFragmentQueries.isBasicCmiModelWithNoSplitServiceFragments(model);
    if (_isBasicCmiModelWithNoSplitServiceFragments) {
      possibleSubsets.add(CmiSubset.BASIC);
    }
    boolean _isSplitCmiModelWithOnlySplitServiceFragments = CmiSplitServiceFragmentQueries.isSplitCmiModelWithOnlySplitServiceFragments(model);
    if (_isSplitCmiModelWithOnlySplitServiceFragments) {
      possibleSubsets.add(CmiSubset.SPLIT);
    }
    boolean _isEmpty = possibleSubsets.isEmpty();
    boolean _not = (!_isEmpty);
    Assert.check(_not, "Model is not in any subset, and thus not a valid CMI model.");
    int _size = possibleSubsets.size();
    boolean _lessThan = (_size < 2);
    Assert.check(_lessThan, 
      (("Model is in multiple subsets: " + possibleSubsets) + ". Subset detection thus has a bug."));
    return IterableExtensions.<CmiSubset>head(possibleSubsets);
  }
}
