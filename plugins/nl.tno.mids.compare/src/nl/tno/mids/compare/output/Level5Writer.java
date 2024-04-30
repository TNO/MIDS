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
import java.util.List;

import nl.tno.mids.compare.MidsCompare;
import nl.tno.mids.compare.data.ComparisonData;
import nl.tno.mids.compare.data.Entity;
import nl.tno.mids.compare.data.Model;
import nl.tno.mids.compare.data.Variant;
import nl.tno.mids.compare.options.CompareOptions;
import nl.tno.mids.compare.output.util.DotSVGGenerator;
import nl.tno.mids.compare.output.util.LatticeGraphGenerator;
import nl.tno.mids.compare.output.util.TemplateUtils;

/** Write relations between model variants (level 5) to files in DOT, SVG and HTML format. */
public class Level5Writer {
    private static final String FILE_NAME = "level5.html";

    private static final String ERROR_TEXT_FILE_NAME = "imageUnavailable.fragment.html";

    /**
     * Generates an HTML view for every entity for which a lattice is generated, and writes these to the entity
     * directories in {@code path}.
     * 
     * @param comparisonData Compare results containing entity variant relations to be rendered.
     * @param compareOptions Options set for this compare run.
     * @param warnings List to collect warnings generated during comparison.
     * @throws IOException In case of an I/O error.
     */
    public static void write(ComparisonData comparisonData, CompareOptions compareOptions, List<String> warnings)
            throws IOException
    {
        for (Entity entity: comparisonData.getEntities()) {
            if (entity.getVariantsWithBehavior().size() > 1) {
                Path entityPath = compareOptions.outputPath.resolve(entity.getName());

                boolean imageWritten = writeModelLatticeImage(entity, entityPath, warnings,
                        compareOptions.svgGenerationTimeout);
                writeEntityHTML(entity, comparisonData.getEntityType().getName(), entityPath, imageWritten);
            }
        }
    }

    /**
     * Write variation lattice for a given entity to a given file.
     * 
     * @param entity Entity to write lattice for.
     * @param outputPath Path to main compare output folder.
     * @param warnings List to collect warnings generated during comparison.
     * @param svgGenerationTimeout The timeout in seconds for generating the SVG. This timeout should be positive.
     * @return {@code true} if SVG image was successfully generated, {@code false} otherwise.
     * @throws IOException In case of an I/O error.
     */
    public static boolean writeModelLatticeImage(Entity entity, Path outputPath, List<String> warnings,
            int svgGenerationTimeout) throws IOException
    {
        // Create lattice representation in DOT format from selected lattice nodes.
        String lattice = LatticeGraphGenerator.generateLattice(entity.getLatticeNodesToDisplay(),
                "\"" + entity.getName() + "\"", entity.getNumber());

        TemplateUtils.write(outputPath, "level5.dot", lattice);
        Path dotPath = outputPath.resolve("level5.dot");
        return DotSVGGenerator.generateDotSVG(outputPath, dotPath, warnings, svgGenerationTimeout);
    }

    /**
     * Generates the view for level 5 and writes it to {@code entityPath}.
     * 
     * @param entity Entity to write view for.
     * @param entityType Type of entity represented by models.
     * @param entityPath Path to entity output folder.
     * @param imageWritten {@code true} if lattice image is present, {@code false} if not.
     * @throws IOException In case of an I/O error.
     */
    protected static void writeEntityHTML(Entity entity, String entityType, Path entityPath, boolean imageWritten)
            throws IOException
    {
        String htmlText = TemplateUtils.getTemplate(FILE_NAME);
        htmlText = htmlText.replace("<!-- #ENTITY_NAME# -->", entity.getNumber() + ": " + entity.getName());
        htmlText = htmlText.replace("<!-- #INCOMPLETE_TEXT# -->", entity.isLatticeIncomplete() ? "(incomplete)" : "");
        htmlText = htmlText.replace("<!-- #VERSION# -->", MidsCompare.VERSION);
        htmlText = htmlText.replace("<!-- #ENTITY_TYPE# -->", entityType);

        List<Variant<Model>> variants = entity.getVariantsWithBehavior();
        StringBuilder variantListStringBuilder = new StringBuilder(variants.size() * 75);
        for (Variant<Model> variant: variants) {
            String variantName = TemplateUtils.renderVariant(variant.getIdentifier());
            variantListStringBuilder.append("<a href=\"" + variantName + ".html\" target=\"_blank\">" + variantName
                    + "<sub>" + entity.getNumber() + "</sub></a>\n");
        }
        htmlText = htmlText.replace("<!-- #VARIANT_LIST# -->", variantListStringBuilder.toString());

        if (imageWritten) {
            htmlText = htmlText.replace("<!-- #IMAGE_CONTENTS# -->", "<img src=\"level5.dot.svg\">");
        } else {
            String warningText = TemplateUtils.getTemplate(ERROR_TEXT_FILE_NAME);
            htmlText = htmlText.replace("<!-- #IMAGE_CONTENTS# -->", warningText);
        }

        TemplateUtils.write(entityPath, FILE_NAME, htmlText);
    }
}
