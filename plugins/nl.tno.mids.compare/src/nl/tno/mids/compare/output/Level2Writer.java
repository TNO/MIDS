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
import java.nio.file.Path;
import java.util.List;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.LatticeNode;
import nl.tno.mids.compare.data.ModelSet;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.output.util.DotSVGGenerator;
import nl.tno.mids.compare.output.util.LatticeGraphGenerator;
import nl.tno.mids.compare.output.util.TemplateUtils;

/** Write relations between model set variants (level 2) to files in DOT, SVG and HTML format. */
public class Level2Writer {
    private static final String FILE_NAME = "level2.html";

    private static final String ERROR_TEXT_FILE_NAME = "imageUnavailable.fragment.html";

    /**
     * Generates the 'level2.html' output file and the related image and writes it to {@code path}.
     * 
     * @param comparisonData Compare results containing model sets with lattice relations.
     * @param compareOptions Options set for this compare run.
     * @param warnings List to collect warnings generated during comparison.
     * @throws IOException In case of an I/O error.
     */
    public static void write(ComparisonData comparisonData, CompareOptions compareOptions, List<String> warnings)
            throws IOException
    {
        Path outputPath = compareOptions.outputPath;
        boolean imageWritten = generateSVG(comparisonData.getLattice(), outputPath, warnings,
                compareOptions.svgGenerationTimeout);
        generateHTML(outputPath, comparisonData.getEntityType().getName(), imageWritten,
                comparisonData.isModelSetLatticeIncomplete());
    }

    /**
     * Write lattice of model set variants as a SVG image.
     * 
     * @param modelSetLatticeNodes Model set lattice nodes to write lattice data for.
     * @param outputPath Path to main compare output folder.
     * @param warnings List to collect warnings generated during comparison.
     * @param svgGenerationTimeout The timeout in seconds for generating the SVG. This timeout should be positive.
     * @return {@code true} if SVG image was successfully generated, {@code false} otherwise.
     * @throws IOException In case of an I/O error.
     */
    static boolean generateSVG(List<LatticeNode<Variant<ModelSet>>> modelSetLatticeNodes, Path outputPath,
            List<String> warnings, int svgGenerationTimeout) throws IOException
    {
        String lattice = LatticeGraphGenerator.generateLattice(modelSetLatticeNodes, "modelSetLattice", 0);

        TemplateUtils.write(outputPath, "level2.dot", lattice);
        Path dotPath = outputPath.resolve("level2.dot");
        return DotSVGGenerator.generateDotSVG(outputPath, dotPath, warnings, svgGenerationTimeout);
    }

    /**
     * Generates the view for level 2 and writes it to {@code path}.
     * 
     * @param outputPath Path to folder to contain the generated HTML file.
     * @param entityType Type of entity represented by models.
     * @param imageWritten {@code true} if lattice image is present, {@code false} if not.
     * @param isModelSetLatticeIncomplete {@code true} if model set lattice is incomplete, {@code false} if not.
     * @throws IOException In case of an I/O error.
     */
    static void generateHTML(Path outputPath, String entityType, boolean imageWritten,
            boolean isModelSetLatticeIncomplete) throws IOException
    {
        String htmlText = TemplateUtils.getTemplate(FILE_NAME);
        htmlText = htmlText.replace("<!-- #INCOMPLETE_TEXT# -->", isModelSetLatticeIncomplete ? "(incomplete)" : "");
        htmlText = htmlText.replace("<!-- #VERSION# -->", MidsCompare.VERSION);
        htmlText = htmlText.replace("<!-- #ENTITY_TYPE# -->", entityType);

        if (imageWritten) {
            htmlText = htmlText.replace("<!-- #IMAGE_CONTENTS# -->", "<img src=\"level2.dot.svg\">");
        } else {
            String warningText = TemplateUtils.getTemplate(ERROR_TEXT_FILE_NAME);
            htmlText = htmlText.replace("<!-- #IMAGE_CONTENTS# -->", warningText);
        }

        TemplateUtils.write(outputPath, FILE_NAME, htmlText);
    }
}
