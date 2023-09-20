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

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.escet.common.java.Lists;
import org.eclipse.escet.common.java.Numbers;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.output.color.ColorUtils;
import nl.tno.mids.compare.output.color.ColorUtils.ColorGradient;
import nl.tno.mids.compare.output.util.TemplateUtils;

/** Write list of model sets with behavior variants (level 1) to a file in HTML format. */
public class Level1Writer {
    private static final ColorGradient BLUE_COLOR_GRADIENT = new ColorGradient(new Color(255, 255, 255),
            new Color(0, 0, 255));

    private static final String FILE_NAME = "level1.html";

    /**
     * Generates the 'level1.html' output file corresponding to comparison data and writes it to {@code outputPath}.
     * 
     * @param comparisonData Compare results containing model sets with variants.
     * @param showComputedModels Configure whether computed model sets are listed.
     * @param outputPath {@link Path} of folder to write output to.
     * @throws IOException In case of an I/O error.
     */
    public static void write(ComparisonData comparisonData, boolean showComputedModels, Path outputPath)
            throws IOException
    {
        List<Integer> uniqueIdentifiersSorted = comparisonData.getModelSetVariants().stream()
                .filter(msv -> !msv.isComputed() || showComputedModels).map(msv -> msv.getIdentifier()).sorted()
                .collect(Collectors.toList());

        int numberOfVariants = Lists.last(uniqueIdentifiersSorted);

        List<ModelSet> modelSetsToShow = showComputedModels ? comparisonData.getModelSets()
                : comparisonData.getInputModelSets();

        String htmlText = TemplateUtils.getTemplate(FILE_NAME);
        htmlText = htmlText.replace("<!-- #VERSION# -->", MidsCompare.VERSION);
        htmlText = htmlText.replace("<!-- #MODEL_SET_VARIANT_DATA# -->",
                constructModelSetVariantTableData(modelSetsToShow, numberOfVariants));
        htmlText = htmlText.replace("<!-- #MODEL_SET_VARIANT_LEGEND_DATA# -->",
                constructLegendData(uniqueIdentifiersSorted));

        TemplateUtils.write(outputPath, FILE_NAME, htmlText);
    }

    /**
     * Construct the HTML text for the table rows, containing the model set variants.
     * 
     * @param modelSets Model sets to display in the table.
     * @param numberOfVariants Total number of model set variants shown in the table
     * @return Constructed table data.
     */
    private static String constructModelSetVariantTableData(List<ModelSet> modelSets, int numberOfVariants) {
        StringBuilder trText = new StringBuilder();
        trText.append(String.format(
                "<tr class=\"align-middle\">\n<td rowspan='%d\'><h6 class='verticalText'>Model set</h6></td>\n</tr>\n",
                modelSets.size() + 1));

        for (ModelSet modelSet: modelSets) {
            trText.append("<tr class=\"align-middle\">\n");
            trText.append(String.format("<td>%s</td>\n", modelSet.getName()));

            Color bg = ColorUtils.getColorSample(BLUE_COLOR_GRADIENT, numberOfVariants,
                    modelSet.getModelSetVariant().getIdentifier());

            Color textColor = ColorUtils.getContrastingColor(bg);

            trText.append(String.format(
                    "<td><div class=\"text-center variant-label\" style=\"margin: 0 auto; background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d)\">%s</div></td>\n",
                    bg.getRed(), bg.getGreen(), bg.getBlue(), textColor.getRed(), textColor.getGreen(),
                    textColor.getBlue(), TemplateUtils.renderVariant(modelSet.getModelSetVariant().getIdentifier())));
            trText.append("</tr>\n");
        }
        return trText.toString();
    }

    /**
     * Construct the legend for the shown model set variants and their colors.
     * 
     * @param uniqueIdentifiersSorted List of model set variant identifiers to be shown in the legend.
     * @return Constructed legend.
     */
    private static String constructLegendData(List<Integer> uniqueIdentifiersSorted) {
        StringBuilder legendData = new StringBuilder();

        int maxEntries = 5;
        int lastVariantIdentifier = Lists.last(uniqueIdentifiersSorted);
        int entriesToShow = Math.min(maxEntries, lastVariantIdentifier - 1);
        Color bg;
        Color textColor;
        for (int variantIdentifier: uniqueIdentifiersSorted.stream().limit(entriesToShow)
                .collect(Collectors.toList()))
        {
            bg = ColorUtils.getColorSample(BLUE_COLOR_GRADIENT, lastVariantIdentifier, variantIdentifier);
            textColor = ColorUtils.getContrastingColor(bg);

            legendData.append(String.format("<tr>\n"
                    + "<td><div class=\"text-center variant-label\" style=\"background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d)\">%s</div></td>\n"
                    + "<td>%s model set variant</td>\n" + "</tr>\n", bg.getRed(), bg.getGreen(), bg.getBlue(),
                    textColor.getRed(), textColor.getGreen(), textColor.getBlue(),
                    TemplateUtils.renderVariant(variantIdentifier), Numbers.toOrdinal(variantIdentifier)));
        }

        if (lastVariantIdentifier - entriesToShow > 1) {
            legendData.append("<tr>\n" + "<td>...</td>\n" + "<td></td>\n" + "</tr>\n");
        }

        bg = BLUE_COLOR_GRADIENT.endColor;
        textColor = ColorUtils.getContrastingColor(bg);
        legendData.append(String.format("<tr>\n"
                + "<td><div class=\"text-center variant-label\" style=\"background-color: rgb(%d, %d, %d); color: rgb(%d, %d, %d)\">%s</div></td>\n"
                + "<td>%s model set variant</td>\n" + "</tr>\n", bg.getRed(), bg.getGreen(), bg.getBlue(),
                textColor.getRed(), textColor.getGreen(), textColor.getBlue(),
                TemplateUtils.renderVariant(lastVariantIdentifier), Numbers.toOrdinal(lastVariantIdentifier)));

        return legendData.toString();
    }
}
