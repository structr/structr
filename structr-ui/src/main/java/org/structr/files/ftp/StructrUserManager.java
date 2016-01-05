/**
 * Copyright (C) 2010-2015 Structr GmbH
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
package org.structr.files.ftp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.rest.auth.AuthHelper;

/**
 *
 *
 */
public class StructrUserManager implements UserManager {

	private static final Logger logger = Logger.getLogger(StructrUserManager.class.getName());

	private SecurityContext securityContext = SecurityContext.getSuperUserInstance();

	@Override
	public User getUserByName(String userName) throws FtpException {
		try (Tx tx = StructrApp.getInstance().tx()) {
			org.structr.web.entity.User structrUser = getStructrUser(userName);
			tx.success();
			if (structrUser != null) {
				return new StructrFtpUser(structrUser);
			} else {
				return null;
			}
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Unable to get user by its name", fex);
		}
		return null;
	}

	@Override
	public String[] getAllUserNames() throws FtpException {

		try (Tx tx = StructrApp.getInstance().tx()) {

			List<String> userNames = new ArrayList();

			Result<Principal> result = Result.EMPTY_RESULT;

			try {

				result = StructrApp.getInstance(securityContext).nodeQuery(Principal.class).getResult();

			} catch (FrameworkException ex) {

				logger.log(Level.WARNING, "Error while searching for principal", ex);

			}

			if (!result.isEmpty()) {

				for (Principal p : result.getResults()) {

					userNames.add(p.getProperty(AbstractUser.name));

				}

			}

			tx.success();
			return (String[]) userNames.toArray(new String[userNames.size()]);
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Unable to get user by its name", fex);
		}
		return null;
	}

	@Override
	public void delete(String string) throws FtpException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void save(User user) throws FtpException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean doesExist(String string) throws FtpException {
		try (Tx tx = StructrApp.getInstance().tx()) {
			boolean exists = (getStructrUser(string) != null);
			tx.success();
			return exists;
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Unable to determine if user " + string + " exists", fex);
		}
		return false;
	}

	@Override
	public User authenticate(Authentication auth) throws AuthenticationFailedException {

		logger.log(Level.INFO, "Authentication: {0}", auth);
		String userName = null;
		String password = null;

		if (auth instanceof UsernamePasswordAuthentication) {

			org.structr.web.entity.User user = null;

			try (Tx tx = StructrApp.getInstance().tx()) {

				UsernamePasswordAuthentication authentication = (UsernamePasswordAuthentication) auth;

				userName = authentication.getUsername();
				password = authentication.getPassword();

				user = (org.structr.web.entity.User) AuthHelper.getPrincipalForPassword(AbstractUser.name, userName, password);

				tx.success();

			} catch (FrameworkException ex) {
				logger.log(Level.WARNING, "FTP authentication attempt failed with username {0} and password {1}", new Object[]{userName, password});
			}

			if (user != null) {

				return new StructrFtpUser(user);

			}

		}

		throw new AuthenticationFailedException("No structr user found for credentials " + userName + "/" + password);
	}

	@Override
	public String getAdminName() throws FtpException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isAdmin(String string) throws FtpException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	private org.structr.web.entity.User getStructrUser(final String userName) {

		try (Tx tx = StructrApp.getInstance().tx()) {

			final org.structr.web.entity.User user = (org.structr.web.entity.User) AuthHelper.getPrincipalForCredential(AbstractUser.name, userName);
			tx.success();
			return user;

		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Unable to get user by its name", fex);
		}

		return null;
	}
}
