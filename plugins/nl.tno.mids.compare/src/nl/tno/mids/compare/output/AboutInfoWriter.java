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

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.escet.common.java.DateTimeUtils;
import org.eclipse.escet.common.java.Sets;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.ModelType;
import nl.tno.mids.compare.output.util.TemplateUtils;

/** Write information about the compare run to a file in HTML format. */
public class AboutInfoWriter {
    private static final String FILE_NAME = "about.html";

    /** Taken from https://icons.getbootstrap.com/icons/info-circle/ */
    private static final String INFO_IMAGE_STRING = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" height=\"16\" fill=\"currentColor\" class=\"bi bi-info-circle\" viewBox=\"0 0 16 16\" focusable=\"false\">\n"
            + "          <path d=\"M8 15A7 7 0 1 1 8 1a7 7 0 0 1 0 14zm0 1A8 8 0 1 0 8 0a8 8 0 0 0 0 16z\"/>\n"
            + "          <path d=\"M8.93 6.588l-2.29.287-.082.38.45.083c.294.07.352.176.288.469l-.738 3.468c-.194.897.105 1.319.808 1.319.545 0 1.178-.252 1.465-.598l.088-.416c-.2.176-.492.246-.686.246-.275 0-.375-.193-.304-.533L8.93 6.588zM9 4.5a1 1 0 1 1-2 0 1 1 0 0 1 2 0z\"/>\n"
            + "        </svg>";

    /**
     * @param comparisonData Compare results containing model set information.
     * @param compareOptions Options set for this compare run.
     * @param startTime Start time of compare run.
     * @param endTime End time of compare run.
     * @param outputPath {@link Path} of folder to write output to.
     * @throws IOException In case of an I/O error.
     */
    public static void write(ComparisonData comparisonData, CompareOptions compareOptions, Instant startTime,
            Instant endTime, Path outputPath) throws IOException
    {
        String htmlText = TemplateUtils.getTemplate(FILE_NAME);

        htmlText = htmlText.replace("<!-- #VERSION# -->", MidsCompare.VERSION);
        htmlText = htmlText.replace("<!-- #START_TIME# -->", renderDatetime(startTime));
        htmlText = htmlText.replace("<!-- #END_TIME# -->", renderDatetime(endTime));
        htmlText = htmlText.replace("<!-- #DURATION# -->",
                DateTimeUtils.durationToString(startTime.until(endTime, ChronoUnit.MILLIS), true));
        htmlText = htmlText.replace("<!-- #MODEL_TYPE# -->", compareOptions.modelType.description);

        if (compareOptions.entityType != null) {
            htmlText = htmlText.replace("<!-- #ENTITY_TYPE# -->", "Configured: " + compareOptions.entityType.getName()
                    + ", " + compareOptions.entityType.getPlural());
        } else {
            htmlText = htmlText.replace("<!-- #ENTITY_TYPE# -->", "Selected based on input: "
                    + comparisonData.getEntityType().getName() + ", " + comparisonData.getEntityType().getPlural());
        }

        htmlText = htmlText.replace("<!-- #COMPARE_MODE# -->",
                (compareOptions.modelType == ModelType.CMI) ? compareOptions.cmiCompareMode.description : "n/a");
        htmlText = htmlText.replace("<!-- #ALGORITHM# -->", compareOptions.compareAlgorithm.getDescription());
        htmlText = htmlText.replace("<!-- #EXTENDED_LATTICE# -->",
                compareOptions.completeLattices ? (compareOptions.showComputedVariants ? "full" : "partial") : "none");
        htmlText = htmlText.replace("<!-- #POST_PROCESSING# -->", Boolean.toString(compareOptions.applyPostprocessing));
        htmlText = htmlText.replace("<!-- #COLOR_SCHEME# -->", compareOptions.colorScheme.description);
        htmlText = htmlText.replace("<!-- #TIMEOUT_SVG# -->",
                DateTimeUtils.durationToString(compareOptions.svgGenerationTimeout * 1000, true));
        htmlText = htmlText.replace("<!-- #UNION_SIZE_LIMIT# -->",
                Integer.toString(compareOptions.unionIntersectionSizeLimit));
        htmlText = htmlText.replace("<!-- #STRUCTURAL_SIZE_LIMIT# -->",
                Integer.toString(compareOptions.structuralCompareSizeLimit));

        htmlText = htmlText.replace("<!-- #MODEL_SETS_LINKS# -->", comparisonData.getModelSets().stream()
                .map(ms -> renderModelSetLink(ms)).collect(Collectors.joining("\n")));

        htmlText = htmlText.replace("<!-- #MODEL_SETS_DATA# -->", comparisonData.getModelSets().stream()
                .map(ms -> renderAboutModelSet(ms)).collect(Collectors.joining("\n")));

        TemplateUtils.write(outputPath, FILE_NAME, htmlText);
    }

    /**
     * Gives a {@link String} representation of the given {@link LocalDateTime}.
     * 
     * @param dateTime The date and time to convert to a {@link String}.
     * @return The converted date and time.
     */
    private static String renderDatetime(Instant dateTime) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("UTC")).format(dateTime);
    }

    private static String renderModelSetLink(ModelSet modelSet) {
        return "<a href=\"javascript:showSelected('" + StringEscapeUtils.escapeHtml(modelSet.getName())
                + "')\" class=\"dropdown-item\">" + modelSet.getName() + "</a>";
    }

    /**
     * Gives a {@link String} representation of information relating to a model set.
     * 
     * @param modelSet Model set to write information for.
     * @return The model set information HTML text.
     */
    private static String renderAboutModelSet(ModelSet modelSet) {
        StringBuilder modelSetString = new StringBuilder();
        modelSetString.append("<div id=\"info-" + StringEscapeUtils.escapeHtml(modelSet.getName())
                + "\" class=\"col-md-8 model-set-info\" style=\"display:none\">\n");
        modelSetString.append("<div class=\"card bg-light mb-3\">\n");
        modelSetString.append("<div class=\"card-header\"><div class=\"row\"><span class=\"text-center\">"
                + modelSet.getName() + "</span></div></div>\n");
        modelSetString.append("<div class=\"card-body\">\n");
        modelSetString.append(descriptionsToTable(modelSet.getDescriptions()) + "\n");
        modelSetString.append("</div>\n");
        modelSetString.append("</div>\n");
        modelSetString.append("</div>\n");

        return modelSetString.toString();
    }

    /**
     * Convert descriptions to HTML list representations.
     * 
     * @param descriptions Descriptions to convert.
     * @return The HTML representation of {@code descriptions}.
     */
    private static String descriptionsToTable(Map<Path, List<String>> descriptions) {
        // If there are no descriptions to convert, return a placeholder.
        if (descriptions.isEmpty()) {
            return "-";
        }

        StringJoiner stringJoiner = new StringJoiner("\n");

        stringJoiner.add("<dl>");
        for (Entry<Path, List<String>> entry: Sets.sortedgeneric(descriptions.entrySet(),
                (e1, e2) -> e1.getKey().compareTo(e2.getKey())))
        {
            stringJoiner.add("<dt>" + renderPath(entry.getKey()) + "</dt>");
            stringJoiner.add("<dd>");
            stringJoiner.add("<ul>");
            for (String line: entry.getValue()) {
                if (isPath(line)) {
                    stringJoiner.add("<li>" + renderPath(Paths.get(line)) + "</li>");
                } else {
                    stringJoiner.add("<li>" + StringEscapeUtils.escapeHtml(line) + "</li>");
                }
            }
            stringJoiner.add("</ul>");
            stringJoiner.add("</dd>");
            stringJoiner.add("</li>");
        }
        stringJoiner.add("</dl>");

        return stringJoiner.toString();
    }

    /**
     * Check if the given string can be interpreted as a path.
     * 
     * @param possPath {@link String} possibly containing a path in text form.
     * 
     * @return {@code true} if the string can be interpreted as a path, {@code false} otherwise.
     */
    private static boolean isPath(String possPath) {
        try {
            Path path = Paths.get(possPath);
            return path.getParent() != null;
        } catch (InvalidPathException e) {
            return false;
        }
    }

    private static String renderPath(Path path) {
        return StringEscapeUtils.escapeHtml(path.getFileName().toString())
                + "<button class=\"btn btn-secondary btn-xs\" data-bs-toggle=\"popover\" title=\"Path\" data-bs-content=\""
                + StringEscapeUtils.escapeHtml(FilenameUtils.separatorsToUnix(path.toString())) + "\">"
                + INFO_IMAGE_STRING + "</button>";
    }
}
