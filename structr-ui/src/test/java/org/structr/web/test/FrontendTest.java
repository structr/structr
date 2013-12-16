/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.test;

import java.util.logging.Level;
import org.structr.web.common.StructrUiTest;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.PropertyMap;
import org.structr.web.auth.UiAuthenticator;
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

		try {
			

			createAdminUser();
			createResourceAccess("_login", UiAuthenticator.NON_AUTH_USER_POST);

			app.beginTx();
			
			String[] args = {"/bin/sh", "-c", "cd src/test/javascript; PATH=$PATH:./bin/`uname`/ casperjs/bin/casperjs --fail-fast test " + testName+ ".js"};

			Process proc = Runtime.getRuntime().exec(args);
			logger.log(Level.INFO, IOUtils.toString(proc.getInputStream()));
			String warnings = IOUtils.toString(proc.getErrorStream());
			
			if (StringUtils.isNotBlank(warnings)) {
				logger.log(Level.WARNING, warnings);
			}
			
			int exitValue = proc.exitValue();
			
			logger.log(Level.INFO, "casperjs subprocess returned with {0}", exitValue);

			app.commitTx();
			
			return exitValue;
			
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
			
		} finally {
			
			app.finishTx();
		}
		
		return 1;

	}

	@Override
	public void test00() {
	}

	protected void createAdminUser() throws FrameworkException {
		
		final PropertyMap properties = new PropertyMap();

		properties.put(User.name, "admin");
		properties.put(User.password, "admin");

		try {
			app.beginTx();
			User user = app.create(User.class, properties);
			user.setProperty(User.password, "admin");
			app.commitTx();
		
		} catch (Throwable t) {

			t.printStackTrace();
			
		} finally {
			
			app.finishTx();
		}
		
	}
	
}
