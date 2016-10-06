/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.test;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.log.ResponseLoggingFilter;
import java.io.IOException;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.User;
import static org.structr.web.test.ResourceAccessTest.createResourceAccess;

//~--- classes ----------------------------------------------------------------
/**
 * Run casperjs frontend tests
 *
 *
 */
public abstract class FrontendTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(FrontendTest.class.getName());
	
	public static final String ADMIN_USERNAME = "admin";
	public static final String ADMIN_PASSWORD = "admin";

	//~--- methods --------------------------------------------------------
	protected int run(final String testName) {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_login", UiAuthenticator.NON_AUTH_USER_POST);
			tx.success();

		} catch (Exception ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			String[] args = {"/bin/sh", "-c", "cd src/test/javascript ; PATH=./bin/`uname`/:$PATH casperjs/bin/casperjs --httpPort=" + httpPort + " test " + testName + ".js"};

			Process proc = Runtime.getRuntime().exec(args);
			logger.info(IOUtils.toString(proc.getInputStream()));
			String warnings = IOUtils.toString(proc.getErrorStream());

			if (StringUtils.isNotBlank(warnings)) {
				logger.warn(warnings);
			}

			final int maxRetries = 60;

			Integer exitValue = 1; // default is error
			try {

				int r = 0;

				while (proc.isAlive() && r < maxRetries) {
					Thread.sleep(1000);
					r++;
				}

				exitValue = proc.exitValue();
				makeVideo(testName);

				return exitValue;

			} catch (IllegalThreadStateException ex) {
				logger.warn("Subprocess has not properly exited", ex);
				logger.warn("", ex);
			}

			logger.info("casperjs subprocess returned with {}", exitValue);

			tx.success();


		} catch (Exception ex) {
			logger.error("", ex);
		}

		return 1;

	}

	private void makeVideo(final String testName) throws IOException {
		String[] args = {"/bin/sh", "-c", "cd ../docs/screenshots &&  avconv -y -r 25 -i " + testName + "/%04d.png -qscale 1 " + testName + ".avi"};
		Process proc = Runtime.getRuntime().exec(args);
		logger.info(IOUtils.toString(proc.getInputStream()));
		String warnings = IOUtils.toString(proc.getErrorStream());
		if (StringUtils.isNotBlank(warnings)) {
			logger.warn(warnings);
		}
	}

	protected User createAdminUser() throws FrameworkException {

		final PropertyMap properties = new PropertyMap();

		properties.put(User.name, ADMIN_USERNAME);
		properties.put(User.password, ADMIN_PASSWORD);
		properties.put(User.isAdmin, true);
		properties.put(User.backendUser, true);

		User user = null;
		
		try (final Tx tx = app.tx()) {

			user = app.create(User.class, properties);
			//user.setProperty(User.password, "admin");
			tx.success();

		} catch (Throwable t) {

			logger.warn("", t);
		}

		return user;

	}

	protected String createEntityAsAdmin(String resource, String... body) {

		StringBuilder buf = new StringBuilder();

		for (String part : body) {
			buf.append(part);
		}

		final Properties config = Services.getBaseConfiguration();

		return getUuidFromLocation(
			RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

			.body(buf.toString())
				.expect().statusCode(201)
			.when().post(resource).getHeader("Location"));
	}

}
