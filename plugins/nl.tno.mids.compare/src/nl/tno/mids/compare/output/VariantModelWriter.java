/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.output;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.escet.cif.io.CifWriter;
import org.eclipse.escet.common.app.framework.AppEnv;

import com.github.tno.gltsdiff.glts.Transition;
import com.github.tno.gltsdiff.glts.lts.automaton.Automaton;
import com.github.tno.gltsdiff.glts.lts.automaton.AutomatonStateProperty;
import com.github.tno.gltsdiff.operators.printers.HtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.SetHtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.StringHtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.TransitionHtmlPrinter;
import com.github.tno.gltsdiff.writers.lts.automaton.AutomatonDotWriter;

import nl.tno.mids.compare.compute.StructuralComparisonUtils;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.RepetitionCount;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.output.util.DotSVGGenerator;
import nl.tno.mids.compare.output.util.TemplateUtils;
import nl.tno.mids.gltsdiff.extensions.AnnotatedProperty;
import nl.tno.mids.gltsdiff.extensions.AnnotatedPropertyHtmlPrinter;

/** Write entity variant models to files in CIF, DOT, SVG and HTML format. */
public class VariantModelWriter {
    private static final String FILE_NAME = "imagePage.html";

    private static final String ERROR_TEXT_FILE_NAME = "imageUnavailable.fragment.html";

    /**
     * Write variant models in compare data.
     * 
     * @param comparisonData Compare results containing entity variants to write.
     * @param compareOptions Options set for this compare run.
     * @param warnings List to collect warnings generated during comparison.
     * @param monitor Monitor to report progress.
     * @throws IOException In case of an I/O error.
     */
    public static void write(ComparisonData comparisonData, CompareOptions compareOptions, List<String> warnings,
            IProgressMonitor monitor) throws IOException
    {
        SubMonitor subMonitor = SubMonitor.convert(monitor, comparisonData.getEntities().size());
        AppEnv.registerSimple();

        try {
            for (Entity entity: comparisonData.getEntities()) {
                subMonitor.split(1);
                writeVariantModelsForEntity(compareOptions.outputPath, entity, warnings,
                        compareOptions.svgGenerationTimeout);
            }
        } finally {
            AppEnv.unregisterApplication();
        }
    }

    /**
     * Write representations for all variants with behavior for a given entity.
     * 
     * @param outputPath {@link Path} to folder containing compare results.
     * @param entity {@link Entity} containing variants to write.
     * @param warnings List to collect warnings generated during comparison.
     * @param svgGenerationTimeout The timeout in seconds for generating the SVG. This timeout should be positive.
     * @throws IOException In case of an I/O error.
     */
    private static void writeVariantModelsForEntity(Path outputPath, Entity entity, List<String> warnings,
            int svgGenerationTimeout) throws IOException
    {
        if (entity.getVariantsWithBehavior().isEmpty()) {
            return;
        }
        Path entityPath = outputPath.resolve(entity.getName());
        Files.createDirectories(entityPath);

        for (Variant<Model> variant: entity.getVariantsWithBehavior()) {
            writeCif(entityPath, variant);
            boolean imageWritten = writeImage(entityPath, variant, warnings, svgGenerationTimeout);
            writeHTML(entityPath, variant, entity.getName(), entity.getNumber(), imageWritten);
        }
    }

    /**
     * Write CIF representations of a given variant.
     * <p>
     * The original CIF specifications are written by this method, i.e., the ones obtained from
     * {@link Model#getOriginalSpecification()}. This is because the structural specifications
     * {@link Model#getStructuralAutomaton()}) do not necessarily have the same language as the original specifications,
     * while the language specifications {@link Model#getLanguageAutomaton()}) may be obtained by computing the state
     * space of the original specification, which one doesn't want to see in the written CIF files. As a consequence,
     * the written CIF files may have data.
     * </p>
     * 
     * @param entityPath {@link Path} to write representations to.
     * @param variant {@link Variant} to write.
     */
    private static void writeCif(Path entityPath, Variant<Model> variant) {
        Path variantPath = entityPath.resolve(TemplateUtils.renderVariant(variant.getIdentifier()) + ".cif");
        CifWriter.writeCifSpec(variant.getValue().getOriginalSpecification(), variantPath.toString(),
                entityPath.toString());
    }

    /**
     * Write state machine model representing a given variant.
     * 
     * @param entityPath {@link Path} to write representations to.
     * @param variant {@link Variant} to write.
     * @param warnings List to collect warnings generated during comparison.
     * @param svgGenerationTimeout The timeout in seconds for generating the SVG. This timeout should be positive.
     * @return {@code true} if SVG image was successfully generated, {@code false} otherwise.
     * @throws IOException In case of an I/O error.
     */
    private static boolean writeImage(Path entityPath, Variant<Model> variant, List<String> warnings,
            int svgGenerationTimeout) throws IOException
    {
        // Convert the state machine model to a structural comparison automaton.
        Automaton<AnnotatedProperty<String, RepetitionCount>> automaton = StructuralComparisonUtils
                .convertToAutomaton(variant.getValue().getStructuralAutomaton());

        // Write the DOT file.
        String variantName = TemplateUtils.renderVariant(variant.getIdentifier());
        Path dotPath = entityPath.resolve(variantName + ".dot");
        HtmlPrinter<Transition<AutomatonStateProperty, AnnotatedProperty<String, RepetitionCount>>> printer = new TransitionHtmlPrinter<>(
                new AnnotatedPropertyHtmlPrinter<>(new StringHtmlPrinter<>(),
                        new SetHtmlPrinter<>(RepetitionCount.getHtmlPrinter(), "", " ", "")));

        try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(dotPath, CREATE_NEW))) {
            new AutomatonDotWriter<>(printer).write(automaton, stream);
        }

        // Convert DOT to SVG image.
        return DotSVGGenerator.generateDotSVG(entityPath, dotPath, warnings, svgGenerationTimeout);
    }

    /**
     * Write view for a given variant.
     * 
     * @param entityPath {@link Path} to write representations to.
     * @param variant {@link Variant} to write.
     * @param entityName Name of entity represented by {@code variant}.
     * @param entityNumber Number of entity represented by {@code variant}.
     * @param imageWritten {@code true} if image is present, {@code false} if not.
     * @throws IOException In case of an I/O error.
     */
    private static void writeHTML(Path entityPath, Variant<Model> variant, String entityName, int entityNumber,
            boolean imageWritten) throws IOException
    {
        String variantName = TemplateUtils.renderVariant(variant.getIdentifier());

        String htmlText = TemplateUtils.getTemplate(FILE_NAME);
        htmlText = htmlText.replace("<!-- #VARIANT_NAME_TITLE# -->", variantName + "/" + entityNumber);
        htmlText = htmlText.replace("<!-- #VARIANT_NAME# -->", variantName + "<sub>" + entityNumber + "</sub>");
        htmlText = htmlText.replace("<!-- #ENTITY_NAME# -->", entityNumber + ": " + entityName);
        if (imageWritten) {
            htmlText = htmlText.replace("<!-- #IMAGE_CONTENTS# -->", "<img src=\"" + variantName + ".dot.svg\">");
        } else {
            String warningText = TemplateUtils.getTemplate(ERROR_TEXT_FILE_NAME);
            htmlText = htmlText.replace("<!-- #IMAGE_CONTENTS# -->", warningText);
        }

        TemplateUtils.write(entityPath, variantName + ".html", htmlText);
    }
}
