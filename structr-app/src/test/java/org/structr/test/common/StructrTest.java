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
package org.structr.test.common;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Methods;
import org.structr.core.api.NamedArguments;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsManager;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.embedded.EmbeddedDatabaseService;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.testng.annotations.*;
import org.testng.annotations.Optional;

import java.io.File;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;

public class StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(StructrTest.class.getName());

	protected static SecurityContext securityContext = null;
	protected static String basePath                 = null;
	protected static App app                         = null;
	protected static String randomTenantId           = RandomStringUtils.secure().nextAlphabetic(10).toUpperCase();
	private boolean first                            = true;

	@BeforeMethod
	protected void starting(Method method) {

		System.out.println("##### Starting " + this.getClass().getName() + "#" + method.getName() + " with tenant identifier " + randomTenantId);
	}

	@AfterMethod
	protected void finished(Method method) {

		System.out.println("##### Finished " + getClass().getName() + "#" + method.getName() + " with tenant identifier " + randomTenantId);
	}

	@BeforeMethod
	public void cleanDatabaseAndSchema() {

		if (!first) {

			try (final Tx tx = app.tx()) {

				final DatabaseService db = Services.getInstance().getDatabaseService();

				// delete everything
				db.cleanDatabase();

				FlushCachesCommand.flushAll();

				tx.success();

			} catch (Throwable t) {

				t.printStackTrace();
				logger.error("Exception while trying to clean database: {}", t.getMessage());
			}

			SchemaService.reloadSchema(new ErrorBuffer(), null, false, false);
			FlushCachesCommand.flushAll();
		}

		first = false;
	}

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	public void setup(@Optional final String testDatabaseConnection) {

		final Date now          = new Date();
		final long timestamp    = now.getTime();

		basePath = "/tmp/structr-test-" + timestamp;

		Settings.Services.setValue("NodeService SchemaService");

		setupDatabaseConnection(testDatabaseConnection);

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.FilesPath.setValue(basePath + "/files");
		Settings.DatabasePath.setValue(basePath + "/db");

		// No graceful shutdown between test classes: the HTTP client keeps its connections alive, so a
		// connector never reports itself drained and every single teardown burnt the whole timeout --
		// about 93 seconds across a full suite run, spent waiting for connections that never close.
		Settings.HttpServiceStopTimeout.setValue(0);

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		while (!services.isInitialized()) {

			try { Thread.sleep(100); } catch (Throwable t) {}
		}

		securityContext = SecurityContext.getSuperUserInstance();
		app = StructrApp.getInstance(securityContext);
	}


	@AfterClass(alwaysRun = true)
	public void teardown() {

		Services.getInstance().shutdown();

		try {

			final File testDir = new File(basePath);
			if (testDir.isDirectory()) {

				FileUtils.deleteDirectory(testDir);

			} else {

				testDir.delete();
			}

		} catch (Throwable t) {

			logger.warn("", t);
		}
	}

	protected void setupDatabaseConnection(String testDatabaseConnection) {

		Settings.DatabaseDriver.setValue(System.getProperty("testDatabaseDriver", Settings.DEFAULT_REMOTE_DATABASE_DRIVER));
		Settings.ConnectionUser.setValue("neo4j");
		Settings.ConnectionPassword.setValue("admin123");

		if (StringUtils.isBlank(testDatabaseConnection)) {

			Settings.ConnectionUrl.setValue(Settings.TestingConnectionUrl.getValue());

		} else {

			Settings.ConnectionUrl.setValue(testDatabaseConnection);
		}

		Settings.ConnectionDatabaseName.setValue("neo4j");
		Settings.TenantIdentifier.setValue(randomTenantId);
	}
}
