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
package org.structr.test.files;

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MigrationService;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.test.web.StructrUiTest;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * An instance whose configured.services does not list the StorageSyncService - every list saved under
 * 6.x, where mounting was opt-in - keeps its mounts standing still, and the startup report is the only
 * place that says so.
 */
public class MountsWithoutSyncServiceTest extends StructrUiTest {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		// the list a 6.x instance saved: no mount service at all
		Settings.Services.setValue("NodeService SchemaService HttpService MigrationService");

		super.setup(testDatabaseConnection);
	}

	@Test
	public void testMountsAreReportedWhenTheSyncServiceIsNotConfigured() {

		final String configured = Settings.Services.getValue();

		try {

			assertEquals("Without mounts there is nothing to report", 0, MigrationService.countMountsWithoutSyncService());

			try (final Tx tx = app.tx()) {

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "standingStill"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY),
						StorageProviderFactory.createConfig("standingStillMount", LocalFSStorageProvider.class, Map.of("mountTarget", "/tmp/structr-unsynchronized-mount")))
				);

				tx.success();
			}

			assertEquals("A mount without the sync service should be reported", 1, MigrationService.countMountsWithoutSyncService());

			// once the service is listed, the same mount is no reason to warn
			Settings.Services.setValue(configured + " StorageSyncService");

			assertEquals("A configured sync service leaves nothing to report", 0, MigrationService.countMountsWithoutSyncService());

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");

		} finally {

			Settings.Services.setValue(configured);
		}
	}
}
