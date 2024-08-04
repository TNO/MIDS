/////////////////////////////////////////////////////////////////////////
// Copyright (c) 2018-2024 TNO and Contributors to the GitHub community
//
// This program and the accompanying materials are made available
// under the terms of the MIT License which is available at
// https://opensource.org/licenses/MIT
//
// SPDX-License-Identifier: MIT
/////////////////////////////////////////////////////////////////////////

package nl.tno.mids.cmi.cmi2yed;

import java.nio.file.Path;

import org.eclipse.escet.cif.metamodel.cif.Specification;
import org.eclipse.escet.common.app.framework.XmlSupport;
import org.eclipse.escet.common.java.Assert;
import org.w3c.dom.Document;

public class CmiToYedTransformer {
    public static void transform(Specification spec, Path outputFileAbsolutePath) {
        // Perform transformation to yEd.
        Document doc = new CmiToYedModelDiagram().transform(spec);
        Assert.notNull(doc);

        // Write yEd file.
        XmlSupport.writeFile(doc, "yEd", outputFileAbsolutePath.toString());
    }
}
