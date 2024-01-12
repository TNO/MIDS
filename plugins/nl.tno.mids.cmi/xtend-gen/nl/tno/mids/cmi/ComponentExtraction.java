package nl.tno.mids.cmi;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import nl.esi.pps.tmsc.Event;
import nl.esi.pps.tmsc.ScopedTMSC;
import nl.esi.pps.tmsc.TMSC;
import nl.tno.mids.cif.extensions.CIFOperations;
import nl.tno.mids.cif.extensions.CifExtensions;
import nl.tno.mids.cif.extensions.FileExtensions;
import nl.tno.mids.cmi.api.protocol.CmiProtocolQueries;
import nl.tno.mids.cmi.cmi2yed.CmiToYedTransformer;
import nl.tno.mids.cmi.postprocessing.PostProcessing;
import nl.tno.mids.cmi.protocol.InferProtocolModel;
import nl.tno.mids.cmi.utils.TmscMetrics;
import nl.tno.mids.pps.extensions.util.TmscFileHelper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.java.DateTimeUtils;
import org.eclipse.escet.common.java.Strings;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.MapExtensions;

@SuppressWarnings("all")
public class ComponentExtraction {
  /**
   * Component extraction using Constructive Model Inference.
   * 
   * @param relativeResolvePath The absolute path of the input file. Either the TMSC or options file.
   *      During model extraction, all paths are to be resolved against the parent folder of this file.
   * @param options The configuration options.
   * @param monitor The progress monitor.
   */
  public void extract(final Path relativeResolvePath, final ComponentExtractionOptions options, final IProgressMonitor monitor) {
    try {
      final LocalDateTime startTime = LocalDateTime.now();
      final ArrayList<String> warnings = new ArrayList<String>();
      final int yEdWork = 10;
      final Path tmscPath = options.getInput().getPath();
      Preconditions.checkArgument(tmscPath.isAbsolute());
      Preconditions.checkArgument(relativeResolvePath.isAbsolute());
      int work = 125;
      int _work = work;
      int _xifexpression = (int) 0;
      boolean _isSaveYed = options.getOutput().isSaveYed();
      if (_isSaveYed) {
        _xifexpression = yEdWork;
      } else {
        _xifexpression = 0;
      }
      work = (_work + _xifexpression);
      final SubMonitor subMonitor = SubMonitor.convert(monitor, work);
      final String tmscName = this.getTmscName(tmscPath);
      subMonitor.split(5);
      subMonitor.subTask("Saving model extraction options");
      this.saveOptions(options);
      subMonitor.split(10);
      subMonitor.subTask(("Loading TMSC from " + tmscName));
      final ScopedTMSC tmsc = TmscFileHelper.loadAndPrepareTMSC(tmscPath, warnings);
      subMonitor.subTask(("Pre-processing TMSC from " + tmscName));
      this.preProcess(tmsc, tmscName, options, subMonitor.split(10));
      subMonitor.split(5);
      subMonitor.subTask("Calculation TMSC metrics");
      final TmscMetrics tmscMetrics = this.getTmscMetrics(tmsc);
      subMonitor.split(50);
      subMonitor.subTask(("Extracting models from TMSC " + tmscName));
      boolean _isSynchronizeDependentTransitions = options.getExtraction().isSynchronizeDependentTransitions();
      final ComponentModelBuilder builder = new ComponentModelBuilder(_isSynchronizeDependentTransitions);
      builder.insert(tmsc);
      Map<String, Specification> modelsMap = builder.getCifModels();
      subMonitor.subTask(("Post-processing CIF models extracted from " + tmscName));
      boolean _isEmpty = options.getPostProcessing().getComponentsInclusionRegEx().trim().isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        final Function2<String, Specification, Boolean> _function = (String k, Specification v) -> {
          return Boolean.valueOf(k.matches(options.getPostProcessing().getComponentsInclusionRegEx()));
        };
        modelsMap = MapExtensions.<String, Specification>filter(modelsMap, _function);
      }
      boolean _isEmpty_1 = options.getPostProcessing().getComponentsExclusionRegEx().trim().isEmpty();
      boolean _not_1 = (!_isEmpty_1);
      if (_not_1) {
        final Function2<String, Specification, Boolean> _function_1 = (String k, Specification v) -> {
          boolean _matches = k.matches(options.getPostProcessing().getComponentsExclusionRegEx());
          return Boolean.valueOf((!_matches));
        };
        modelsMap = MapExtensions.<String, Specification>filter(modelsMap, _function_1);
      }
      modelsMap = PostProcessing.postProcess(modelsMap, relativeResolvePath, options.getPostProcessing().getOperations(), 
        subMonitor.split(10));
      subMonitor.split(5);
      if ((Objects.equal(options.getOutput().getOutputMode(), OutputMode.COMPONENTS) && (!options.getExtraction().isModelPerComponent()))) {
        subMonitor.subTask("Combining component models");
        this.combineComponentModels(modelsMap);
      }
      final OutputMode outputMode = options.getOutput().getOutputMode();
      if (outputMode != null) {
        switch (outputMode) {
          case COMPONENTS:
            subMonitor.subTask("Saving CIF models");
            this.saveCifModels(modelsMap, options, subMonitor.split(30));
            break;
          case PROTOCOL:
            subMonitor.subTask("Inferring protocol");
            final Specification protocolModel = InferProtocolModel.createProtocol(options.getOutput().getProtocolName1(), 
              options.getOutput().getProtocolName2(), options.getOutput().getScope(), modelsMap, subMonitor.split(29));
            modelsMap.clear();
            modelsMap.put(CmiProtocolQueries.getProtocolName(protocolModel), protocolModel);
            this.saveCifModels(modelsMap, options, subMonitor.split(1));
            break;
          default:
            break;
        }
      }
      boolean _isSaveYed_1 = options.getOutput().isSaveYed();
      if (_isSaveYed_1) {
        subMonitor.split(yEdWork);
        final Path outputFolderAbsolutePath = this.createOutputFolder(options);
        final BiConsumer<String, Specification> _function_2 = (String modelName, Specification spec) -> {
          final Path outputFileAbsolutePath = outputFolderAbsolutePath.resolve(Strings.fmt("%s.graphml", modelName));
          CmiToYedTransformer.transform(spec, outputFileAbsolutePath);
        };
        modelsMap.forEach(_function_2);
      }
      final LocalDateTime endTime = LocalDateTime.now();
      this.saveReport(options, tmscMetrics, warnings, startTime.until(endTime, ChronoUnit.MILLIS));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected void saveReport(final ComponentExtractionOptions options, final TmscMetrics tmscMetrics, final List<String> warnings, final long runtimeMs) {
    final Path targetFolder = this.createOutputFolder(options);
    final Path targetFile = targetFolder.resolve("component-extraction-report.txt");
    try (final PrintWriter writer = new Function0<PrintWriter>() {
      @Override
      public PrintWriter apply() {
        try {
          File _file = targetFile.toFile();
          FileOutputStream _fileOutputStream = new FileOutputStream(_file);
          return new PrintWriter(_fileOutputStream);
        } catch (Throwable _e) {
          throw Exceptions.sneakyThrow(_e);
        }
      }
    }.apply()) {
      writer.format(Locale.US, "TMSC duration: %s\n", 
        tmscMetrics.duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase());
      writer.format(Locale.US, "TMSC events:   %,d\n", Long.valueOf(tmscMetrics.eventCount));
      writer.format(Locale.US, "Running time:  %s\n", DateTimeUtils.durationToString(runtimeMs, true));
      boolean _isEmpty = warnings.isEmpty();
      boolean _not = (!_isEmpty);
      if (_not) {
        writer.append("Warnings:\n");
        for (final String warning : warnings) {
          writer.format(Locale.US, " - %s\n", warning);
        }
      }
    } catch (final Throwable _t) {
      if (_t instanceof IOException) {
        final IOException e = (IOException)_t;
        throw new WrappedException("Failed to save report to file.", e);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  protected Path saveOptions(final ComponentExtractionOptions options) {
    Path _xblockexpression = null;
    {
      final Path targetFolder = this.createOutputFolder(options);
      Path _xtrycatchfinallyexpression = null;
      try {
        _xtrycatchfinallyexpression = options.writeOptionsFile(targetFolder);
      } catch (final Throwable _t) {
        if (_t instanceof IOException) {
          final IOException e = (IOException)_t;
          throw new WrappedException("Failed to save options to file.", e);
        } else {
          throw Exceptions.sneakyThrow(_t);
        }
      }
      _xblockexpression = _xtrycatchfinallyexpression;
    }
    return _xblockexpression;
  }
  
  protected Path createOutputFolder(final ComponentExtractionOptions options) {
    try {
      final Path targetFolder = options.getOutput().getPath();
      Files.createDirectories(targetFolder);
      return targetFolder;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private void preProcess(final TMSC tmsc, final String tmscName, final ComponentExtractionOptions options, final IProgressMonitor monitor) {
    try {
      final SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
      subMonitor.subTask(("Excluding components: " + tmscName));
      subMonitor.split(1);
      final int noEvts = tmsc.getEvents().size();
      subMonitor.split(1);
      subMonitor.split(1);
      int _size = tmsc.getEvents().size();
      boolean _notEquals = (_size != noEvts);
      if (_notEquals) {
        subMonitor.subTask(("Saving pre-processed TMSC: " + tmscName));
        final Path targetFolder = this.createOutputFolder(options);
        final Path targetFile = targetFolder.resolve((tmscName + "-preprocessed.tmscz"));
        TmscFileHelper.saveTMSC(tmsc, targetFile);
      }
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private Specification combineComponentModels(final Map<String, Specification> modelsMap) {
    Specification _xblockexpression = null;
    {
      final Specification mergedSpec = CIFOperations.mergeSpecifications(modelsMap.values());
      modelsMap.clear();
      _xblockexpression = modelsMap.put("allComponents", mergedSpec);
    }
    return _xblockexpression;
  }
  
  private void saveCifModels(final Map<String, Specification> namedSpecs, final ComponentExtractionOptions options, final IProgressMonitor monitor) {
    final SubMonitor subMonitor = SubMonitor.convert(monitor, namedSpecs.size());
    final Path targetFolder = this.createOutputFolder(options);
    final BiConsumer<String, Specification> _function = (String name, Specification cif) -> {
      try {
        subMonitor.split(1);
        final Path targetFile = targetFolder.resolve((name + ".cif"));
        CifExtensions.normalizeOrder(cif);
        FileExtensions.saveCIF(cif, targetFile);
      } catch (Throwable _e) {
        throw Exceptions.sneakyThrow(_e);
      }
    };
    namedSpecs.forEach(_function);
  }
  
  private String getTmscName(final Path tmscPath) {
    String tmscName = IterableExtensions.<Path>last(tmscPath).toString();
    final int idx = tmscName.lastIndexOf(".");
    if ((idx >= 0)) {
      tmscName = tmscName.substring(0, idx);
    }
    return tmscName;
  }
  
  private TmscMetrics getTmscMetrics(final TMSC tmsc) {
    final TmscMetrics tmscMetrics = new TmscMetrics();
    Long firstTimestamp = null;
    Long lastTimestamp = null;
    Collection<Event> _events = tmsc.getEvents();
    for (final Event event : _events) {
      {
        tmscMetrics.eventCount++;
        if ((firstTimestamp == null)) {
          firstTimestamp = event.getTimestamp();
          lastTimestamp = event.getTimestamp();
        } else {
          firstTimestamp = Long.valueOf(Long.min((firstTimestamp).longValue(), (event.getTimestamp()).longValue()));
          lastTimestamp = Long.valueOf(Long.max((lastTimestamp).longValue(), (event.getTimestamp()).longValue()));
        }
      }
    }
    tmscMetrics.duration = Duration.ofNanos(((lastTimestamp).longValue() - (firstTimestamp).longValue()));
    return tmscMetrics;
  }
}
