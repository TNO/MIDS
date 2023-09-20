/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2023 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.compare.output.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utility for adding required assets to output. */
public class AssetController {
    /**
     * Generates any assets supporting the generated HTML output, like for example Javascript and CSS files.
     * 
     * @param path Destination path for the assets directory that is to be created.
     * @throws IOException Thrown whenever the generation of the asset files fails.
     */
    public static void generateHTMLAssets(Path path) throws IOException {
        // Create assets directory.
        Path assetsDst = path.resolve("assets");
        Files.createDirectories(assetsDst);

        // Copy Javascript and CSS resource assets.
        copyAsset(assetsDst, "bootstrap.min.css");
        copyAsset(assetsDst, "bootstrap.bundle.min.js");
        copyAsset(assetsDst, "jquery-3.6.0.slim.min.js");
    }

    /**
     * Helper method to copy an asset {@code assetName} from the resources of this package to a destination indicated by
     * {@code assetPath}.
     * 
     * @param assetPath Destination path of the asset.
     * @param assetName Name of the asset to copy.
     * @throws IOException Thrown whenever reading or writing the indicated asset fails.
     */
    private static void copyAsset(Path assetPath, String assetName) throws IOException {
        String assetsDir = "/" + AssetController.class.getPackage().getName().replace('.', '/').replaceAll("/util$",
                "/resources/assets/");

        Files.copy(AssetController.class.getResourceAsStream(assetsDir + assetName), assetPath.resolve(assetName));
    }
}
