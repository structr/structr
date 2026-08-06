/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.ComponentConfiguration;
import org.testng.annotations.Test;

import java.nio.file.Path;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

public class Deployment7Test extends DeploymentTestBase {

	@Test
	public void test70ExportComponentConfigurationWithoutComponent() {

		// A ComponentConfiguration whose related DOM node (component) has been
		// deleted leaves the "domNode" relationship empty. The deployment export
		// must not fail with a NullPointerException in that case (see
		// DeployCommand.exportComponentConfigurations).

		String configUuid = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface node   = app.create(StructrTraits.COMPONENT_CONFIGURATION, "test70");
			final ComponentConfiguration config = node.as(ComponentConfiguration.class);

			configUuid = config.getUuid();

			// sanity check: this configuration has no related component
			assertNull("Test setup should create a component configuration without a component", config.getComponent());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		assertNotNull(configUuid);

		Path exportPath = null;

		try {

			// this used to throw a NullPointerException because the export
			// dereferenced config.getComponent().getUuid() unconditionally
			exportPath = doExport();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Deployment export must not fail for a component configuration without a component: " + t.getMessage());

		} finally {

			if (exportPath != null) {

				try {

					deleteExportAt(exportPath);
				} catch (Exception ignore) {}
			}
		}
	}
}