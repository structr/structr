/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.Tx;
import org.structr.schema.SchemaService;
import org.testng.annotations.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Date;

/**
 *
 */
public class LicensingTest {

	private static final Logger logger = LoggerFactory.getLogger(LicensingTest.class.getName());

	protected SecurityContext securityContext = null;
	protected String basePath                 = null;
	protected App app                         = null;
	protected String randomTenantId           = RandomStringUtils.randomAlphabetic(10).toUpperCase();

	@BeforeMethod
	protected void starting(final Method method) {

		System.out.println("######################################################################################");
		System.out.println("# Starting " + getClass().getName() + "#" + method.getName());
		System.out.println("######################################################################################");
	}

	@AfterMethod
	protected void finished(final Method method) {

		System.out.println("######################################################################################");
		System.out.println("# Finished " + getClass().getName() + "#" + method.getName());
		System.out.println("######################################################################################");
	}

	@AfterMethod
	public void cleanDatabaseAndSchema() {

		Services.disableIndexConfiguration();

		try (final Tx tx = app.tx()) {

			// delete everything
			Services.getInstance().getDatabaseService().cleanDatabase();

			FlushCachesCommand.flushAll();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			logger.error("Exception while trying to clean database: {}", t.getMessage());
		}


		try {

			FlushCachesCommand.flushAll();

			SchemaService.ensureBuiltinTypesExist(app);

		} catch (Throwable t) {

			t.printStackTrace();
			logger.error("Exception while trying to create built-in schema for tenant identifier {}: {}", randomTenantId, t.getMessage());
		}
	}

	@Parameters("testDatabaseConnection")
	@BeforeClass
	public void startSystem(@Optional String testDatabaseConnection) {

		Services.disableTestingMode();
		Services.disableIndexConfiguration();

		final Date now          = new Date();
		final long timestamp    = now.getTime();

		basePath = "/tmp/structr-test-" + timestamp;

		Settings.Services.setValue("NodeService LogService SchemaService");

		setupDatabaseConnection(testDatabaseConnection);

		// example for new configuration setup
		Settings.BasePath.setValue(basePath);
		Settings.FilesPath.setValue(basePath + "/files");

		Settings.RelationshipCacheSize.setValue(1000);
		Settings.NodeCacheSize.setValue(1000);

		Settings.SuperUserName.setValue("superadmin");
		Settings.SuperUserPassword.setValue("sehrgeheim");

		final Services services = Services.getInstance();

		// wait for service layer to be initialized
		do {
			try {
				Thread.sleep(100);
			} catch (Throwable t) {
			}

		} while (!services.isInitialized());

		securityContext = SecurityContext.getSuperUserInstance();
		app = StructrApp.getInstance(securityContext);
	}

	@AfterClass
	public void stopSystem() {

		Services.getInstance().shutdown();

		try {
			File testConf = new File("structr.conf");
			if (testConf.isFile()) {
				testConf.delete();
			}

		} catch (Throwable t) {
			logger.warn("", t);
		}


		try {
			File testDir = new File(basePath);
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

		// use database driver from system property, default to MemoryDatabaseService
		Settings.DatabaseDriver.setValue(System.getProperty("testDatabaseDriver", Settings.DEFAULT_DATABASE_DRIVER));
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
