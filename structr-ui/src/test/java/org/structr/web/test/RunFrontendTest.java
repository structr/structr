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

import org.structr.web.common.StructrUiTest;

//~--- JDK imports ------------------------------------------------------------

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
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
public class RunFrontendTest extends StructrUiTest {

	private static final Logger logger = Logger.getLogger(RunFrontendTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	public void test01Run() {
		
		try {
			try {
				createAdminUser();
				createResourceAccess("_login", UiAuthenticator.NON_AUTH_USER_POST);
				
			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, "Could not create admin user", ex);
			}
			
			String[] args = {"/bin/sh", "-c", "cd src/test/javascript;./run_tests.sh"};
			Process proc = Runtime.getRuntime().exec(args);
			logger.log(Level.INFO, IOUtils.toString(proc.getInputStream()));
			logger.log(Level.WARNING, IOUtils.toString(proc.getErrorStream()));
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}

	private void createAdminUser() throws FrameworkException {
		
		final CreateNodeCommand cmd  = Services.command(securityContext, CreateNodeCommand.class);
		final PropertyMap properties = new PropertyMap();

		properties.put(AbstractNode.type, User.class.getSimpleName());
		properties.put(User.name, "admin");
		properties.put(User.password, "admin");
	
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<User>() {

			@Override
			public User execute() throws FrameworkException {

				return (User)cmd.execute(properties);
			}		
		});
		
	}
	
}
