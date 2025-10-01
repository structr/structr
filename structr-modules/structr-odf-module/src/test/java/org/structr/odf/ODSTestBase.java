/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.odf;

import org.structr.core.property.StartNode;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.odf.entity.relationship.ODFExporterEXPORTS_TOFile;
import org.structr.odf.entity.relationship.ODFExporterGETS_TRANSFORMATION_FROMVirtualType;
import org.structr.odf.entity.relationship.ODFExporterUSES_TEMPLATEFile;
import org.structr.odf.traits.definitions.ODFExporterTraitDefinition;
import org.structr.odf.traits.definitions.ODSExporterTraitDefinition;
import org.structr.odf.traits.definitions.ODTExporterTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.BeforeMethod;

public class ODSTestBase extends StructrUiTest {

	@BeforeMethod(firstTimeOnly = true)
	public void createSchema() {

		StructrTraits.registerTrait(new ODFExporterEXPORTS_TOFile());
		StructrTraits.registerTrait(new ODFExporterGETS_TRANSFORMATION_FROMVirtualType());
		StructrTraits.registerTrait(new ODFExporterUSES_TEMPLATEFile());

		StructrTraits.registerRelationshipType(StructrTraits.ODF_EXPORTER_EXPORTS_TO_FILE,                       StructrTraits.ODF_EXPORTER_EXPORTS_TO_FILE);
		StructrTraits.registerRelationshipType(StructrTraits.ODF_EXPORTER_GETS_TRANSFORMATION_FROM_VIRTUAL_TYPE, StructrTraits.ODF_EXPORTER_GETS_TRANSFORMATION_FROM_VIRTUAL_TYPE);
		StructrTraits.registerRelationshipType(StructrTraits.ODF_EXPORTER_USES_TEMPLATE_FILE,                    StructrTraits.ODF_EXPORTER_USES_TEMPLATE_FILE);

		StructrTraits.registerTrait(new ODFExporterTraitDefinition());
		StructrTraits.registerTrait(new ODSExporterTraitDefinition());
		StructrTraits.registerTrait(new ODTExporterTraitDefinition());

		StructrTraits.registerNodeType(StructrTraits.ODF_EXPORTER, StructrTraits.ODF_EXPORTER);
		StructrTraits.registerNodeType(StructrTraits.ODS_EXPORTER, StructrTraits.ODF_EXPORTER, StructrTraits.ODS_EXPORTER);
		StructrTraits.registerNodeType(StructrTraits.ODT_EXPORTER, StructrTraits.ODF_EXPORTER, StructrTraits.ODT_EXPORTER);

		final TraitsInstance rootInstance = TraitsManager.getRootInstance();

		// register ODFExporter -> File relationships
		Traits.getTrait(StructrTraits.FILE).registerPropertyKey(new StartNode(rootInstance, "exportFileOfExporter", StructrTraits.ODF_EXPORTER_EXPORTS_TO_FILE));
		Traits.getTrait(StructrTraits.FILE).registerPropertyKey(new StartNode(rootInstance, "templateFileOfExporter", StructrTraits.ODF_EXPORTER_USES_TEMPLATE_FILE));

		// create new schema instance that includes the modified root schema
		TraitsManager.replaceCurrentInstance(TraitsManager.createCopyOfRootInstance());
	}
}
