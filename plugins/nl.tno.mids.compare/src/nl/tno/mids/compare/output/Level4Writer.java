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

import static nl.tno.mids.compare.output.color.ColorUtils.getColorSample;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.Lists;
import org.eclipse.escet.common.java.Numbers;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.output.color.ColorUtils;
import nl.tno.mids.compare.output.color.ColorUtils.ColorGradient;
import nl.tno.mids.compare.output.util.TemplateUtils;

/** Write matrix displaying which entity has which variant in each model set (level 4) to a file in HTML format. */
public class Level4Writer {
    private static final ColorGradient BLUE_COLOR_GRADIENT = new ColorGradient(new Color(255, 255, 255),
            new Color(0, 0, 255));

    private static final Color NO_BEHAVIOR_COLOR = new Color(249, 115, 109);

    private static final String FILE_NAME = "level4.html";

    /**
     * Write entity variants per model set in HTML format.
     * 
     * @param comparisonData Compare results containing entity variants to write.
     * @param showComputedModels Configure whether computed model sets are listed.
     * @param path {@link Path} to file to use for output.
     * @throws IOException In case of an I/O error.
     */
    public static void write(ComparisonData comparisonData, boolean showComputedModels, Path path) throws IOException {
        List<Entity> entities = new ArrayList<>(comparisonData.getEntities());

        List<Integer> uniqueIdentifiersSorted = comparisonData.getEntities().stream()
                .flatMap(c -> c.getVariantsWithBehavior().stream().map(v -> v.getIdentifier())).sorted().distinct()
                .collect(Collectors.toList());

        int maxIdentifier = Lists.last(uniqueIdentifiersSorted);

        List<ModelSet> modelSetsToShow = showComputedModels ? comparisonData.getModelSets()
                : comparisonData.getInputModelSets();

        String htmlText = TemplateUtils.getTemplate(FILE_NAME);
        htmlText = htmlText.replace("<!-- #ENTITY_VARIANT_HEADER# -->", constructTableHeader(modelSetsToShow));
        htmlText = htmlText.replace("<!-- #ENTITY_TYPE_PLURAL# -->", comparisonData.getEntityType().getPlural());
        htmlText = htmlText.replace("<!-- #VERSION# -->", MidsCompare.VERSION);

        htmlText = htmlText.replace("<!-- #ENTITY_VARIANT_DATA# -->", constructTableData(path.getParent(), entities,
                modelSetsToShow, maxIdentifier, comparisonData.getEntityType()));
        htmlText = htmlText.replace("<!-- #ENTITY_VARIANT_LEGEND_DATA# -->",
                constructLegendData(uniqueIdentifiersSorted, maxIdentifier, comparisonData.getEntityType()));

        TemplateUtils.write(path, FILE_NAME, htmlText);
    }

    /**
     * Construct header line containing all known model set names.
     * 
     * @param modelSets Model sets to be shown in this matrix.
     * @return {@String} containing constructed header line.
     */
    private static String constructTableHeader(List<ModelSet> modelSets) {
        StringBuilder sb = new StringBuilder();
        long numberOfColumns = modelSets.size();

        sb.append(String.format("" //
                + "\n<tr>\n"//
                + "<td rowspan=\"2\" colspan=\"3\"></td>\n" //
                + "<td class=\"text-center\" colspan=\"%d\"><h6>Model set</h6></td>\n" //
                + "</tr>\n", //
                numberOfColumns));

        sb.append("<tr>\n");
        sb.append(modelSets.stream()
                .map(ms -> String.format(
                        "<td class=\"text-center\" style=\"width:42px\"><span class=\"verticalText\">%s</span></td>\n",
                        ms.getName()))
                .collect(Collectors.joining()));
        sb.append("</tr>\n");
        return sb.toString();
    }

    /**
     * Construct the HTML text for the table rows containing the entity variants.
     * 
     * @param outputPath {@link Path} to output folder.
     * @param entities {@link List} of entities.
     * @param modelSets Model sets to be shown in this matrix.
     * @param maxIdentifier Highest variation identifier present in the model sets.
     * @param entityType The entity type in the comparison.
     * @return The constructed table data.
     */
    private static String constructTableData(Path outputPath, List<Entity> entities, List<ModelSet> modelSets,
            int maxIdentifier, EntityType entityType)
    {
        StringBuilder trText = new StringBuilder();
        trText.append(String.format("<tr class=\"align-middle\">\n<td rowspan=\"%d\"><h6 class=\"verticalText\">"
                + entityType.getCapitalizedName() + "</h6></td>\n</tr>\n", entities.size() + 1));
        for (Entity entity: entities) {
            trText.append("<tr class=\"align-middle\">\n");
            trText.append(String.format("<td>%d</td>\n", entity.getNumber()));
            trText.append(String.format("<td>%s</td>\n", entity.getName()));
            for (ModelSet modelSet: modelSets) {
                Model model = modelSet.getEntityModel(entity.getName());
                if (!model.hasBehavior()) {
                    Color bg = NO_BEHAVIOR_COLOR;
                    Color textColor = ColorUtils.getContrastingColor(bg);
                    trText.append(String.format(
                            "<td style='background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d)'>"
                                    + "<div class=\"text-center\">-</div></td>\n",
                            bg.getRed(), bg.getGreen(), bg.getBlue(), textColor.getRed(), textColor.getGreen(),
                            textColor.getBlue()));
                } else {
                    trText.append(constructTableCell(entity.getName(), entity.getNumber(),
                            model.getVariant().getIdentifier(), maxIdentifier));
                }
            }
            trText.append("</tr>\n");
        }
        return trText.toString();
    }

    /**
     * Construct the HTML text for a table cell describing a variant in a model set.
     * 
     * @param entityName Name of the entity the variant belongs to.
     * @param number Number of the entity in the table.
     * @param cellValue Identifier of variant of entity in model set.
     * @param maxIdentifier Highest variation identifier present in the model sets.
     * @return The constructed table cell.
     */
    private static String constructTableCell(String entityName, int number, Integer cellValue, int maxIdentifier) {
        StringBuilder cellData = new StringBuilder();

        Color bg = getColorSample(BLUE_COLOR_GRADIENT, maxIdentifier, cellValue);
        Color textColor = ColorUtils.getContrastingColor(bg);

        cellData.append(
                String.format("<td style='background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d);'>", bg.getRed(),
                        bg.getGreen(), bg.getBlue(), textColor.getRed(), textColor.getGreen(), textColor.getBlue()));

        String variantId = TemplateUtils.renderVariant(cellValue);
        String formattedVariant = String.format(
                "<div class=\"text-center\" style='cursor: pointer;' "
                        + "onclick=\"window.open('%s/%s.html', '_blank')\">%s<sub>%d</sub></div>",
                entityName, variantId, variantId, number);
        cellData.append(formattedVariant);
        cellData.append("</td>\n");

        return cellData.toString();
    }

    /**
     * Construct a legend explaining the coloring of the table cells.
     * 
     * @param uniqueIdentifiersSorted Unique variant identifiers present in the model sets.
     * @param maxIdentifier Highest variation identifier present in the model sets.
     * @param entityType Type of entity being compared.
     * @return The constructed legend.
     */
    private static String constructLegendData(List<Integer> uniqueIdentifiersSorted, int maxIdentifier,
            EntityType entityType)
    {
        StringBuilder legendData = new StringBuilder();

        Color bg = NO_BEHAVIOR_COLOR;
        Color textColor = ColorUtils.getContrastingColor(bg);
        legendData.append(String.format("\n<tr>\n"
                + "<td><div class=\"text-center variant-label\" style=\"background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d)\">-</div></td>\n"
                + "<td>No behavior for " + entityType.getName() + "</td>\n" + "</tr>\n", bg.getRed(), bg.getGreen(),
                bg.getBlue(), textColor.getRed(), textColor.getGreen(), textColor.getBlue()));

        int maxEntries = 5;
        int entriesToShow = Math.min(maxEntries, maxIdentifier - 1);
        for (int variantIdentifier: uniqueIdentifiersSorted.stream().limit(entriesToShow)
                .collect(Collectors.toList()))
        {
            bg = getColorSample(BLUE_COLOR_GRADIENT, maxIdentifier, variantIdentifier);
            textColor = ColorUtils.getContrastingColor(bg);

            legendData.append(String.format("<tr>\n"
                    + "<td><div class=\"text-center variant-label\" style=\"background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d)\">%s<sub><em>n</em></sub></div></td>\n"
                    + "<td>%s behavior for %s <em>n</em></td>\n</tr>\n", bg.getRed(), bg.getGreen(), bg.getBlue(),
                    textColor.getRed(), textColor.getGreen(), textColor.getBlue(),
                    TemplateUtils.renderVariant(variantIdentifier), Numbers.toOrdinal(variantIdentifier),
                    entityType.getName()));
        }

        if (maxIdentifier - entriesToShow > 1) {
            legendData.append("<tr>\n" + "<td>...</td>\n" + "<td></td>\n" + "</tr>\n");
        }

        bg = BLUE_COLOR_GRADIENT.endColor;
        textColor = ColorUtils.getContrastingColor(bg);
        legendData.append(String.format("<tr>\n"
                + "<td><div class=\"text-center variant-label\" style=\"background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d)\">%s<sub><em>n</em></sub></div></td>\n"
                + "<td>%s behavior for %s <em>n</em></td>\n" + "</tr>\n", bg.getRed(), bg.getGreen(), bg.getBlue(),
                textColor.getRed(), textColor.getGreen(), textColor.getBlue(),
                TemplateUtils.renderVariant(maxIdentifier), Numbers.toOrdinal(maxIdentifier), entityType.getName()));

        return legendData.toString();
    }
}
