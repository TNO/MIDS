/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
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
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

import com.github.tno.gltsdiff.glts.State;
import com.github.tno.gltsdiff.glts.Transition;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomaton;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffAutomatonStateProperty;
import com.github.tno.gltsdiff.glts.lts.automaton.diff.DiffProperty;
import com.github.tno.gltsdiff.operators.printers.HtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.TransitionHtmlPrinter;
import com.github.tno.gltsdiff.operators.printers.lts.automaton.diff.DiffPropertyHtmlPrinter;
import com.github.tno.gltsdiff.writers.lts.automaton.diff.DiffAutomatonDotWriter;
import com.google.common.base.Preconditions;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.MidsTransitionProperty;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.output.util.DotSVGGenerator;
import nl.tno.mids.compare.output.util.TemplateUtils;

/** Write differences between model variants (level 6) to files in DOT, SVG and HTML format. */
public class Level6Writer {
    private static final String FILE_NAME = "level6.html";

    private static final String ERROR_TEXT_FILE_NAME = "imageUnavailable.fragment.html";

    /**
     * Write structural compare results for variants for a specific entity.
     * 
     * @param sourceVariant Source variant to write structural differences for.
     * @param targetVariant Target variant to write structural differences for.
     * @param entity Entity that contains the source and target variants.
     * @param diff Difference automaton to write.
     * @param options Settings controlling compare behavior.
     * @param entityType Type of entity to be written.
     * @param warnings List to collect warnings generated during comparison.
     * @throws IOException In case of an I/O error.
     */
    public static void write(Variant<Model> sourceVariant, Variant<Model> targetVariant, Entity entity,
            DiffAutomaton<MidsTransitionProperty> diff, CompareOptions options, EntityType entityType,
            List<String> warnings) throws IOException
    {
        Preconditions.checkArgument(
                sourceVariant.getValue().getEntityName().equals(targetVariant.getValue().getEntityName()),
                "Invalid structural difference, variants belong to different " + entityType.getPlural() + ".");
        Preconditions.checkArgument(sourceVariant.getValue().getEntityName().equals(entity.getName()),
                "Invalid " + entityType.getName() + " provided.");
        String entityName = entity.getName();

        // Create output folder if necessary.
        Path entityPath = options.outputPath.resolve(entityName);
        if (!Files.exists(entityPath)) {
            Files.createDirectories(entityPath);
        }

        // Create SVG image representing 'diff'.
        String sourceVariantName = TemplateUtils.renderVariant(sourceVariant.getIdentifier());
        String targetVariantName = TemplateUtils.renderVariant(targetVariant.getIdentifier());

        boolean fileWritten = createSVG(diff, warnings, entityPath, sourceVariantName, targetVariantName,
                options.svgGenerationTimeout);

        // Write HTML wrapper for generated SVG image.
        writeHTML(diff, entityPath, sourceVariantName, targetVariantName, entityName, entity.getNumber(),
                entityType.getName(), fileWritten);
    }

    /**
     * Create SVG image representing structural differences.
     * 
     * @param diff Difference automaton to write.
     * @param warnings List to collect warnings generated during comparison.
     * @param entityPath {@link Path} to entity output folder.
     * @param sourceVariantName Name of source variant of structural differences.
     * @param targetVariantName Name of target variant of structural differences.
     * @param svgGenerationTimeout The timeout in seconds for generating the SVG. This timeout should be positive.
     * @return {@code true} if SVG image was successfully generated, {@code false} otherwise.
     * @throws IOException In case of an I/O error.
     */
    private static boolean createSVG(DiffAutomaton<MidsTransitionProperty> diff, List<String> warnings, Path entityPath,
            String sourceVariantName, String targetVariantName, int svgGenerationTimeout) throws IOException
    {
        // Write the DOT file.
        String diffFileName = sourceVariantName + "_vs_" + targetVariantName + ".dot";
        Path diffDotPath = entityPath.resolve(diffFileName);
        HtmlPrinter<Transition<DiffAutomatonStateProperty, DiffProperty<MidsTransitionProperty>>> printer = new TransitionHtmlPrinter<>(
                new DiffPropertyHtmlPrinter<>(MidsTransitionProperty.PRINTER));

        try (OutputStream stream = new BufferedOutputStream(Files.newOutputStream(diffDotPath, CREATE_NEW))) {
            new DiffAutomatonDotWriter<>(printer).write(diff, stream);
        }

        // Convert DOT to SVG image.
        boolean fileWritten = DotSVGGenerator.generateDotSVG(entityPath, diffDotPath, warnings, svgGenerationTimeout);
        return fileWritten;
    }

    /**
     * Write HTML wrapper for generated SVG image.
     * 
     * @param diff Difference automaton to write.
     * @param entityPath {@link Path} to entity output folder.
     * @param sourceVariant Name of source variant of structural differences.
     * @param targetVariant Name of target variant of structural differences.
     * @param entityName Name of entity structural differences relate to.
     * @param entityNumber Number of entity structural differences relate to.
     * @param entityType Type of entity to be written.
     * @param imageWritten {@code true} if structural differences image is present, {@code false} if not.
     * @throws IOException In case of an I/O error.
     */
    private static void writeHTML(DiffAutomaton<MidsTransitionProperty> diff, Path entityPath, String sourceVariant,
            String targetVariant, String entityName, int entityNumber, String entityType, boolean imageWritten)
            throws IOException
    {
        String htmlText = TemplateUtils.getTemplate(FILE_NAME);
        htmlText = htmlText.replace("<!-- #SOURCE_VARIANT_TITLE# -->", sourceVariant + "/" + entityNumber);
        htmlText = htmlText.replace("<!-- #TARGET_VARIANT_TITLE# -->", targetVariant + "/" + entityNumber);
        htmlText = htmlText.replace("<!-- #SOURCE_VARIANT# -->", sourceVariant + "<sub>" + entityNumber + "</sub>");
        htmlText = htmlText.replace("<!-- #TARGET_VARIANT# -->", targetVariant + "<sub>" + entityNumber + "</sub>");
        htmlText = htmlText.replace("<!-- #ENTITY_NAME# -->", entityNumber + ": " + entityName);
        htmlText = htmlText.replace("<!-- #VERSION# -->", MidsCompare.VERSION);
        htmlText = htmlText.replace("<!-- #ENTITY_TYPE# -->", entityType);
        if (imageWritten) {
            String svgImage = Files.lines(entityPath.resolve(sourceVariant + "_vs_" + targetVariant + ".dot.svg"))
                    .collect(Collectors.joining());
            htmlText = htmlText.replace("<!-- #IMAGE_CONTENTS# -->", svgImage);

            StringJoiner offcanvasDataJoiner = new StringJoiner("\n");
            for (State<DiffAutomatonStateProperty> diffState: diff.getStates()) {
                int index = 0;
                for (Transition<?, DiffProperty<MidsTransitionProperty>> transition: diff
                        .getOutgoingTransitions(diffState))
                {
                    offcanvasDataJoiner.add(writeOffCanvasForTransition(diff, transition, index++, entityType));
                }
            }

            htmlText = htmlText.replace("<!-- #OFFCANVAS_DATA# -->", offcanvasDataJoiner.toString());
        } else {
            String warningText = TemplateUtils.getTemplate(ERROR_TEXT_FILE_NAME);
            htmlText = htmlText.replace("<!-- #IMAGE_CONTENTS# -->", warningText);
            htmlText = htmlText.replace("<!-- #OFFCANVAS_DATA# -->", "");
        }

        TemplateUtils.write(entityPath, sourceVariant + "_vs_" + targetVariant + ".html", htmlText);
    }

    /**
     * Write information on transition in HTML format.
     * 
     * @param diff Difference automaton containing transition to describe.
     * @param transition Transition to describe.
     * @param index Index of transition.
     * @param entityType Type of entity to be written.
     * @return String containing description of transition in HTML format.
     */
    private static String writeOffCanvasForTransition(DiffAutomaton<MidsTransitionProperty> diff,
            Transition<?, DiffProperty<MidsTransitionProperty>> transition, int index, String entityType)
    {
        String eventSymbol = StringEscapeUtils.escapeHtml(transition.getProperty().getProperty().getProperty());
        String transitionId = (transition.getSource().getId() + 1) + "-" + index + "-"
                + (transition.getTarget().getId() + 1);

        StringJoiner offCanvasJoiner = new StringJoiner("\n");

        offCanvasJoiner.add(
                "<div class=\"offcanvas-header\" style=\"display:none\" id=\"offcanvas-header-" + transitionId + "\">");
        offCanvasJoiner.add(
                "<h5 class=\"offcanvas-title\" id=\"offcanvas-label-" + transitionId + "\">" + eventSymbol + "</h5>");
        offCanvasJoiner.add(
                "<button type=\"button\" class=\"btn-close text-reset\" data-bs-dismiss=\"offcanvas\" aria-label=\"Close\"></button>");
        offCanvasJoiner.add("</div>");
        offCanvasJoiner.add(
                "<div class=\"offcanvas-body\" style=\"display:none\" id=\"offcanvas-body-" + transitionId + "\" >");
        long count = diff
                .countTransitions(property -> property.getProperty().getProperty().getProperty().equals(eventSymbol));
        offCanvasJoiner.add("<div id=\"offcanvas-count-" + transitionId + "\">The event occurs " + count + " time"
                + (count != 1 ? "s" : "") + " in the " + entityType + ".</div>");
        offCanvasJoiner.add("</div>");

        return offCanvasJoiner.toString();
    }
}
