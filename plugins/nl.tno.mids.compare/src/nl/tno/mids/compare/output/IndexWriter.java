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

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import net.automatalib.commons.util.Pair;
import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.output.util.TemplateUtils;

/** Write compare overview page to a file in HTML format. */
public class IndexWriter {
    private static final String FILE_NAME = "index.html";

    private static final String WARNING_TEXT_FILE_NAME = "index_warnings.fragment.html";

    private static final String LEVEL6_TEXT_MATRICES_FILE_NAME = "index_level6_matrices.fragment.html";

    /**
     * Generates the 'index.html' output file corresponding to comparison data and writes it to {@code path}.
     * 
     * @param comparisonData Compare results containing results to be shown on the index page.
     * @param showComputedModels Configure whether computed models should be present in the matrix.
     * @param outputPath {@link Path} of folder to write output to.
     * @param warnings List to collect warnings generated during comparison.
     * @throws IOException In case of an I/O error.
     */
    public static void write(ComparisonData comparisonData, boolean showComputedModels, Path outputPath,
            List<String> warnings) throws IOException
    {
        // Get all entities that have at least two variants with behavior.
        List<Entity> entities = comparisonData.getEntities().stream()
                .filter(c -> (c.getVariantsWithBehavior().size() > 1)).collect(Collectors.toList());

        // Get all unique identifiers of the variants, per entity.
        Map<String, List<Integer>> uniqueIdentifiers = new HashMap<>();

        for (Entity entity: entities) {
            List<Integer> identifiers = entity.getVariantsWithBehavior().stream()
                    .filter(m -> !m.isComputed() || showComputedModels).map(c -> c.getIdentifier()).sorted().distinct()
                    .collect(Collectors.toList());

            uniqueIdentifiers.put(entity.getName(), identifiers);
        }

        // Find the pairs of variant identifiers of all variant pairs
        // that have previously been written for every entity.
        SortedMap<Entity, Set<Pair<Integer, Integer>>> variantPairs = new TreeMap<>();

        for (Entity entity: entities) {
            Set<Pair<Integer, Integer>> pairs = entity.getStructuralDifferencePairs().stream()
                    .map(p -> Pair.of(p.getFirst().getIdentifier(), p.getSecond().getIdentifier()))
                    .collect(Collectors.toSet());

            variantPairs.put(entity, pairs);
        }

        Set<Entity> selectedEntities = variantPairs.keySet();

        // Put data into template.
        String htmlText = TemplateUtils.getTemplate(FILE_NAME);
        htmlText = htmlText.replace("<!-- #VERSION# -->", MidsCompare.VERSION);
        htmlText = htmlText.replace("<!-- #ENTITY_TYPE# -->", comparisonData.getEntityType().getCapitalizedName());
        htmlText = htmlText.replace("<!-- #OVERVIEW_LEVEL5_LIST# -->", makeLevel5List(selectedEntities));
        htmlText = htmlText.replace("<!-- #OVERVIEW_LEVEL6_LIST# -->", makeLevel6List(selectedEntities));
        htmlText = htmlText.replace("<!-- #OVERVIEW_LEVEL6_MATRICES# -->",
                makeLevel6Matrices(uniqueIdentifiers, variantPairs));
        htmlText = htmlText.replace("<!-- #OVERVIEW_WARNINGS# -->", makeWarningList(warnings));

        // Write result to disk.
        TemplateUtils.write(outputPath, FILE_NAME, htmlText);
    }

    /**
     * Generate the list of entities for the level 5 drop-down menu.
     * 
     * @param selectedEntities Entities to be included in the menu.
     * @return The level 5 drop-down menu HTML text.
     */
    private static String makeLevel5List(Set<Entity> selectedEntities) {
        StringBuilder level5ListBuilder = new StringBuilder();

        level5ListBuilder.append("\n");
        for (Entity entity: selectedEntities) {
            level5ListBuilder.append(
                    "<a href=\"" + entity.getName() + "/level5.html\" class=\"dropdown-item\" target=\"_blank\">\n"
                            + entity.getNumber() + ": " + entity.getName() + "</a>");
        }

        return level5ListBuilder.toString();
    }

    /**
     * Generate a variant matrix for a list of entities.
     * 
     * @param variants {@link Map} linking entities to identifiers of variants.
     * @param variantPairs {@link Map} linking entities to pairs of variants for which structural differences have been
     *     written.
     * @return The level 6 matrices HTML text.
     */
    private static String makeLevel6Matrices(Map<String, List<Integer>> variants,
            SortedMap<Entity, Set<Pair<Integer, Integer>>> variantPairs)
    {
        StringBuilder level6Matrices = new StringBuilder();

        String level6MatrixTemplate = TemplateUtils.getTemplate(LEVEL6_TEXT_MATRICES_FILE_NAME);

        for (Entry<Entity, Set<Pair<Integer, Integer>>> entityEntry: variantPairs.entrySet()) {
            Entity entity = entityEntry.getKey();
            String entityName = entity.getName();
            int entityNumber = entity.getNumber();
            String level6Matrix = level6MatrixTemplate.replace("<!-- #MATRIX_ENTITY_NAME# -->", entityName);
            level6Matrix = level6Matrix.replace("<!-- #MATRIX_ENTITY_LABEL# -->", entityNumber + ": " + entityName);

            StringBuilder matrixHeaderTextBuilder = new StringBuilder();
            for (Integer variant: variants.get(entityName)) {
                matrixHeaderTextBuilder.append("<th scope=\"col\" class=\"var-diff-cell\">"
                        + TemplateUtils.renderVariant(variant) + "<sub>" + entityNumber + "</sub></th>\n");
            }

            level6Matrix = level6Matrix.replace("<!-- #MATRIX_HEADER# -->", matrixHeaderTextBuilder.toString());

            StringBuilder matrixBodyTextBuilder = new StringBuilder();
            for (Integer row: variants.get(entityName)) {
                matrixBodyTextBuilder.append("<tr>\n");
                matrixBodyTextBuilder.append("<th scope=\"row\" class=\"var-diff-cell\">"
                        + TemplateUtils.renderVariant(row) + "<sub>" + entityNumber + "</sub></th>\n");
                for (Integer column: variants.get(entityName)) {
                    if (entityEntry.getValue().contains(Pair.of(row, column))) {
                        matrixBodyTextBuilder.append("<td class=\"var-diff-cell\">");
                        matrixBodyTextBuilder.append("<a href=\"" + entityName + "/" + TemplateUtils.renderVariant(row)
                                + "_vs_" + TemplateUtils.renderVariant(column)
                                + ".html\" class=\"btn btn-link\" target=\"_blank\">diff</a>");
                        matrixBodyTextBuilder.append("</td>\n");
                    } else {
                        matrixBodyTextBuilder.append("<td class=\"var-diff-cell\">-</td>\n");
                    }
                }
                matrixBodyTextBuilder.append("</tr>\n");
            }
            level6Matrix = level6Matrix.replace("<!-- #MATRIX_BODY# -->", matrixBodyTextBuilder.toString());

            level6Matrices.append(level6Matrix);
        }

        return level6Matrices.toString();
    }

    /**
     * Generate the list of entities for the level 6 drop-down menu.
     * 
     * @param entities Entities to be included in the menu.
     * @return The level 6 drop-down menu HTML text.
     */
    private static String makeLevel6List(Set<Entity> entities) {
        StringBuilder level6ListBuilder = new StringBuilder();

        level6ListBuilder.append("\n");
        for (Entity entity: entities) {
            level6ListBuilder.append("<a href=\"javascript:showSelected('" + entity.getName()
                    + "')\" class=\"dropdown-item\">\n" + entity.getNumber() + ": " + entity.getName() + "</a>");
        }

        return level6ListBuilder.toString();
    }

    /**
     * Generate a HTML list containing collected warnings.
     * 
     * @param warnings List to collect warnings generated during comparison.
     * @return The warnings list HTML text.
     */
    private static String makeWarningList(List<String> warnings) {
        String warningList = "";

        if (!warnings.isEmpty()) {
            String warningTemplate = TemplateUtils.getTemplate(WARNING_TEXT_FILE_NAME);

            StringBuilder warningTextBuilder = new StringBuilder();

            for (String warning: warnings) {
                warningTextBuilder.append("\n<li>\n" + warning + "</li>");
            }

            warningList = "\n" + warningTemplate.replace("<!-- #WARNINGS_LIST# -->", warningTextBuilder.toString());
            warningList = warningList.replace("<!-- #WARNINGS_SIZE# -->", Integer.toString(warnings.size()));
        }

        return warningList;
    }
}
