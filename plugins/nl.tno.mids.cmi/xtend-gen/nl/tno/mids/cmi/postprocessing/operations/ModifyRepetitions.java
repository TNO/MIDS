package nl.tno.mids.cmi.postprocessing.operations;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import nl.tno.mids.cif.extensions.mrr.MrrModifyUtils;
import nl.tno.mids.cif.extensions.mrr.cif.CifMrrLetter;
import nl.tno.mids.cif.extensions.mrr.cif.CifToMrr;
import nl.tno.mids.cif.extensions.mrr.cif.CifToMrrConfig;
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCif;
import nl.tno.mids.cif.extensions.mrr.cif.MrrToCifMode;
import nl.tno.mids.cif.extensions.mrr.data.MRR;
import nl.tno.mids.cif.extensions.mrr.data.MrrWithWord;
import nl.tno.mids.cif.extensions.mrr.data.RepetitionMRR;
import nl.tno.mids.cmi.postprocessing.PostProcessingModel;
import nl.tno.mids.cmi.postprocessing.PostProcessingModelCifSpec;
import nl.tno.mids.cmi.postprocessing.PostProcessingOperation;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingPreconditionSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingResultSubset;
import nl.tno.mids.cmi.postprocessing.status.PostProcessingStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.xtend.lib.annotations.Accessors;

/**
 * Modify repetitions. Uses {@link MRR Minimal Repetition Representations (MRRs)}.
 */
@Accessors
@SuppressWarnings("all")
public class ModifyRepetitions extends PostProcessingOperation<ModifyRepetitionsOptions> {
  @Override
  public PostProcessingPreconditionSubset getPreconditionSubset() {
    return new PostProcessingPreconditionSubset(Boolean.valueOf(false), Boolean.valueOf(false));
  }
  
  @Override
  public PostProcessingResultSubset getResultSubset() {
    final boolean useData = Objects.equal(this.options.mode, MrrToCifMode.DATA);
    return new PostProcessingResultSubset(Boolean.valueOf(useData), Boolean.valueOf(useData));
  }
  
  @Override
  public void applyOperation(final Map<String, PostProcessingModel> models, final Set<String> selectedComponents, final Path relativeResolvePath, final IProgressMonitor monitor) {
    final SubMonitor subMonitor = SubMonitor.convert(monitor, selectedComponents.size());
    for (final String component : selectedComponents) {
      {
        final PostProcessingModel model = models.get(component);
        this.getPreconditionSubset().ensureSubset(model);
        final Specification cifSpec = model.getCifSpec();
        ModifyRepetitions.modifyRepetitions(cifSpec, this.options, subMonitor.split(1));
        PostProcessingStatus _resultStatus = this.getResultStatus(model.status);
        PostProcessingModelCifSpec _postProcessingModelCifSpec = new PostProcessingModelCifSpec(cifSpec, component, _resultStatus);
        models.put(component, _postProcessingModelCifSpec);
      }
    }
  }
  
  private static void modifyRepetitions(final Specification specification, final ModifyRepetitionsOptions options, final IProgressMonitor monitor) {
    final SubMonitor subMonitor = SubMonitor.convert(monitor, 100);
    subMonitor.subTask("Computing repetitions");
    final CifToMrrConfig config = new CifToMrrConfig(1, 1);
    final List<MrrWithWord<CifMrrLetter>> mrrWithWords = CifToMrr.cifToMrr(specification, config, subMonitor.split(97));
    subMonitor.subTask("Applying repetitions modifications");
    subMonitor.split(1);
    Preconditions.checkArgument((!(options.makeInfinite && (options.maxRepeats > 0))));
    Preconditions.checkArgument((!((options.upperThreshold > 0) && (options.lowerThreshold > options.upperThreshold))));
    final Predicate<RepetitionMRR<CifMrrLetter>> _function = (RepetitionMRR<CifMrrLetter> r) -> {
      return (((options.lowerThreshold == 0) || (r.getCount() >= options.lowerThreshold)) && ((options.upperThreshold == 0) || (r.getCount() <= options.upperThreshold)));
    };
    final Predicate<RepetitionMRR<CifMrrLetter>> tresholdFilter = _function;
    for (final MrrWithWord<CifMrrLetter> mrrWithWord : mrrWithWords) {
      if (options.makeInfinite) {
        MrrModifyUtils.<CifMrrLetter>mrrSetInfiniteRepeat(mrrWithWord.mrr, tresholdFilter);
      } else {
        if ((options.maxRepeats > 0)) {
          MrrModifyUtils.<CifMrrLetter>mrrRestrictMaxRepeat(mrrWithWord.mrr, options.maxRepeats, tresholdFilter);
        }
      }
    }
    subMonitor.split(2);
    for (final MrrWithWord<CifMrrLetter> mrrWithWord_1 : mrrWithWords) {
      MrrToCif.mrrToCif(mrrWithWord_1, options.mode);
    }
  }
  
  public ModifyRepetitions(final ModifyRepetitionsOptions options) {
    super(options);
  }
}
