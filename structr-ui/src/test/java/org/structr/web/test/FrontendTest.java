/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.test;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
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
 * @author Axel Morgner
 */
public class FrontendTest extends StructrUiTest {

	private static final Logger logger = Logger.getLogger(FrontendTest.class.getName());

	//~--- methods --------------------------------------------------------
	protected int run(final String testName) {

		try (final Tx tx = app.tx()) {

			createAdminUser();
			createResourceAccess("_login", UiAuthenticator.NON_AUTH_USER_POST);
			tx.success();

		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		try (final Tx tx = app.tx()) {

			// Workaround to remove local storage, as phantomjs is pretty buggy here.
			// Currently, phantomjs doesn't allow localStorage to be modified remotely,
			// and the --local-storage-path parameter is ignored.
			String[] args = {"/bin/sh", "-c", "rm ~/.qws/share/data/Ofi\\ Labs/PhantomJS/* ; cd src/test/javascript ; PATH=./bin/`uname`/:$PATH casperjs/bin/casperjs --local-storage-path=" + basePath + " test " + testName + ".js"};
			//String[] args = {"/bin/sh", "-c", "rm ~/.qws/share/data/Ofi\\ Labs/PhantomJS/* ; cd src/test/javascript ; PATH=$PATH:./bin/`uname`/ casperjs/bin/casperjs --debug test " + testName + ".js"};

			Process proc = Runtime.getRuntime().exec(args);
			logger.log(Level.INFO, IOUtils.toString(proc.getInputStream()));
			String warnings = IOUtils.toString(proc.getErrorStream());

			if (StringUtils.isNotBlank(warnings)) {
				logger.log(Level.WARNING, warnings);
			}

			int exitValue = proc.exitValue();

			logger.log(Level.INFO, "casperjs subprocess returned with {0}", exitValue);

			tx.success();

			makeVideo(testName);

			return exitValue;

		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return 1;

	}

	@Override
	public void test00() {
	}

	private void makeVideo(final String testName) throws IOException {
		String[] args = {"/bin/sh", "-c", "cd ../docs/screenshots &&  avconv -y -r 25 -i " + testName + "/%04d.png -qscale 1 " + testName + ".avi"};
		Process proc = Runtime.getRuntime().exec(args);
		logger.log(Level.INFO, IOUtils.toString(proc.getInputStream()));
		String warnings = IOUtils.toString(proc.getErrorStream());
		if (StringUtils.isNotBlank(warnings)) {
			logger.log(Level.WARNING, warnings);
		}
	}

	protected void createAdminUser() throws FrameworkException {

		final PropertyMap properties = new PropertyMap();

		properties.put(User.name, "admin");
		properties.put(User.password, "admin");
		properties.put(User.isAdmin, true);

		try (final Tx tx = app.tx()) {

			User user = app.create(User.class, properties);
			//user.setProperty(User.password, "admin");
			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
		}

	}

}
