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

import java.awt.Color;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.Strings;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.options.EntityType;
import nl.tno.mids.compare.options.HslColorScheme;
import nl.tno.mids.compare.output.color.ColorUtils;
import nl.tno.mids.compare.output.color.HSLColor;
import nl.tno.mids.compare.output.color.HslColorSampler;
import nl.tno.mids.compare.output.util.TemplateUtils;

/** Write matrix describing differences between model sets (level 3) to a file in HTML format. */
public class Level3Writer {
    private static final Color COMPARED_TO_ITSELF_COLOR = Color.GRAY;

    private static final String FILE_NAME = "level3.html";

    /**
     * Generates the 'level3.html' output file corresponding to comparison data and writes it to {@code path}.
     * 
     * @param comparisonData Compare results containing model sets to be shown in the matrix.
     * @param compareOptions Options set for this compare run.
     * @throws IOException In case of an I/O error.
     */
    public static void write(ComparisonData comparisonData, CompareOptions compareOptions) throws IOException {
        List<ModelSet> modelSetsToShow = compareOptions.showComputedVariants ? comparisonData.getModelSets()
                : comparisonData.getInputModelSets();

        Map<String, Integer> modelSetDifferences = new HashMap<>();
        for (ModelSet modelSet: modelSetsToShow) {
            for (ModelSet otherModelSet: modelSetsToShow) {
                modelSetDifferences.put(getDifferenceKey(modelSet, otherModelSet),
                        ModelSet.getDifferenceCount(modelSet, otherModelSet));
            }
        }

        int maxModelSetDifference = modelSetDifferences.values().stream().max(Comparator.comparing(Integer::intValue))
                .get();

        String htmlText = TemplateUtils.getTemplate(FILE_NAME);
        htmlText = htmlText.replace("<!-- #MODEL_SET_HEADER# -->", constructTableHeader(modelSetsToShow));
        htmlText = htmlText.replace("<!-- #VERSION# -->", MidsCompare.VERSION);
        htmlText = htmlText.replace("<!-- #MODEL_SET_DATA# -->", constructTableData(modelSetsToShow,
                modelSetDifferences, compareOptions.colorScheme, maxModelSetDifference));
        htmlText = htmlText.replace("<!-- #MODEL_SET_LEGEND_SPECTRUM# -->",
                constructLegendData(compareOptions.colorScheme, maxModelSetDifference, comparisonData.getEntityType()));

        TemplateUtils.write(compareOptions.outputPath, FILE_NAME, htmlText);
    }

    /**
     * Construct header line containing all known model set names.
     * 
     * @param modelSets Model sets that should be related in matrix.
     * @return {@String} containing constructed header line.
     */
    private static String constructTableHeader(List<ModelSet> modelSets) {
        StringBuilder builder = new StringBuilder();

        builder.append("\n<tr>\n");
        builder.append("<td rowspan=\"2\" colspan=\"2\"></td>\n");
        builder.append(
                Strings.fmt("<td colspan=\"%d\" class=\"text-center\"><h6>Model set</h6></td>\n", modelSets.size()));
        builder.append("</tr>\n");

        String modelSetNames = modelSets.stream()
                .map(ms -> String.format(
                        "<td class=\"text-center\" style=\"width:50px\"><span class=\"verticalText\">%s</span></td>\n",
                        ms.getName()))
                .collect(Collectors.joining());

        builder.append(Strings.fmt("<tr>\n%s</tr>\n", modelSetNames));

        return builder.toString();
    }

    /**
     * Construct table containing all known model sets.
     * 
     * @param modelSets Model sets to be visualized.
     * @param modelSetDifferences Mapping of model set pairs to number of differences between them.
     * @param colorScheme Color scheme to be used to render table.
     * @param maxModelSetDifference Highest number of differences between model sets.
     * @return {@String} containing constructed table data.
     */
    private static String constructTableData(List<ModelSet> modelSets, Map<String, Integer> modelSetDifferences,
            HslColorScheme colorScheme, int maxModelSetDifference)
    {
        HslColorSampler colorSampler = colorScheme.getSampler();
        StringBuilder tableData = new StringBuilder();

        // Write the vertical "Model Sets" heading
        tableData.append("<tr>\n");
        tableData.append(Strings.fmt("<td rowspan=\"%d\" class=\"align-middle\">", 1 + modelSets.size()));
        tableData.append("<div class=\"verticalText\"><h6>Model set</h6></div>");
        tableData.append("</td>\n");
        tableData.append("</tr>\n");

        for (ModelSet modelSet: modelSets) {
            tableData.append("<tr class=\"align-middle\">\n");
            tableData.append(Strings.fmt("<td>%s</td>\n", modelSet.getName()));

            for (ModelSet otherModelSet: modelSets) {
                int modelSetNameDifference = Boolean.compare(modelSet.getModelSetVariant().isComputed(),
                        otherModelSet.getModelSetVariant().isComputed());
                if (!modelSet.getModelSetVariant().isComputed() && !otherModelSet.getModelSetVariant().isComputed()) {
                    modelSetNameDifference = modelSet.getName().compareTo(otherModelSet.getName());
                }
                if (modelSet.getModelSetVariant().isComputed() && otherModelSet.getModelSetVariant().isComputed()) {
                    modelSetNameDifference = modelSet.getModelSetVariant().getIdentifier()
                            - otherModelSet.getModelSetVariant().getIdentifier();
                }

                int modelSetDifference = modelSetDifferences.get(getDifferenceKey(modelSet, otherModelSet));

                String difference;
                HSLColor hslBg;
                if (modelSetNameDifference > 0) {
                    difference = "";
                    hslBg = new HSLColor(Color.WHITE);
                } else if (modelSetNameDifference == 0) {
                    difference = "=";
                    hslBg = new HSLColor(COMPARED_TO_ITSELF_COLOR);
                } else {
                    difference = Integer.toString(modelSetDifference);
                    hslBg = colorSampler.sampleColor(modelSetDifference, maxModelSetDifference);
                }

                Color textColor = ColorUtils.getContrastingColor(hslBg.getRGB());
                tableData.append(String.format(
                        "<td><div class=\"text-center\" style=\"background-color: hsl(%d, %d%%, %d%%); color: rgb(%d, %d, %d)\">%s</div></td>\n",
                        (int)hslBg.getHue(), (int)hslBg.getSaturation(), (int)hslBg.getLuminance(), textColor.getRed(),
                        textColor.getGreen(), textColor.getBlue(), difference));
            }

            tableData.append("</tr>\n");
        }

        return tableData.toString();
    }

    /**
     * Create key uniquely describing two model sets.
     * 
     * @param modelSet First model set to be described in the key.
     * @param otherModelSet Second model set to be described in the key.
     * @return Key uniquely describing two model sets.
     */
    private static String getDifferenceKey(ModelSet modelSet, ModelSet otherModelSet) {
        return String.format("%s_%s", modelSet.getName(), otherModelSet.getName());
    }

    /**
     * Construct a legend explaining the coloring of the table cells.
     * 
     * @param colorScheme Color scheme to be used to render table.
     * @param maxModelSetDifference Highest number of differences between model sets.
     * @param entityType The type of entity in the comparison.
     * @return The constructed legend.
     */
    private static String constructLegendData(HslColorScheme colorScheme, int maxModelSetDifference,
            EntityType entityType)
    {
        StringBuilder legendData = new StringBuilder();

        HslColorSampler colorSampler = colorScheme.getSampler();
        if (maxModelSetDifference < 9) {
            for (int i = 0; i <= maxModelSetDifference; i++) {
                legendData.append(createLegendRow(colorSampler, i, maxModelSetDifference, entityType));
            }
        } else {
            double step = maxModelSetDifference / 4.0;
            for (int i = 0; i < 4; i++) {
                int colorValue = (int)Math.round(i * step);
                legendData.append(createLegendRow(colorSampler, colorValue, maxModelSetDifference, entityType));
                legendData.append("<tr>\n" + "<td>...</td>\n" + "<td></td>\n" + "</tr>\n");
            }

            legendData.append(createLegendRow(colorSampler, maxModelSetDifference, maxModelSetDifference, entityType));
        }

        return legendData.toString();
    }

    /**
     * Construct legend row for given value.
     * 
     * @param colorSampler Color sampler to be used to render table.
     * @param differenceCount Difference value to be shown in legend row.
     * @param maxModelSetDifference Highest value to be shown in legend.
     * @param entityType The type of entity in the comparison.
     * @return The constructed legend row.
     */
    private static String createLegendRow(HslColorSampler colorSampler, int differenceCount, int maxModelSetDifference,
            EntityType entityType)
    {
        Color bg = colorSampler.sampleColor(differenceCount, maxModelSetDifference).getRGB();
        Color textColor = ColorUtils.getContrastingColor(bg);

        String data = String.format("<tr>\n"
                + "<td><div class=\"text-center variant-label\" style=\"background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d)\">%d</div></td>\n"
                + "<td>%s</td>\n" + "</tr>\n", bg.getRed(), bg.getGreen(), bg.getBlue(), textColor.getRed(),
                textColor.getGreen(), textColor.getBlue(), differenceCount,
                renderLegendLabel(differenceCount, entityType));
        return data;
    }

    /**
     * Construct a legend label for a given value.
     * 
     * @param differenceCount Difference value to be described in the label.
     * @param entityType The type of entity in the comparison.
     * @return The constructed legend label.
     */
    private static String renderLegendLabel(int differenceCount, EntityType entityType) {
        if (differenceCount == 1) {
            return "1 " + entityType.getName() + " with different behavior";
        } else {
            return differenceCount + " " + entityType.getPlural() + " with different behavior";
        }
    }
}
