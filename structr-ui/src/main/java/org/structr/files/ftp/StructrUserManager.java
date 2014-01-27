/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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
 * GNU General Public License for more details.
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
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Principal;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;

/**
 *
 * @author axel
 */
public class StructrUserManager implements UserManager {

	private static final Logger logger = Logger.getLogger(StructrUserManager.class.getName());
	
	private SecurityContext securityContext = SecurityContext.getSuperUserInstance();
	
	@Override
	public User getUserByName(String userName) throws FtpException {
		org.structr.web.entity.User structrUser = getStructrUser(userName);
		if (structrUser != null) {
			return new StructrFtpUser(structrUser);
		} else {
			return null;
		}
	}

	@Override
	public String[] getAllUserNames() throws FtpException {

		List<String> userNames = new ArrayList();
		
		Result<Principal> result = Result.EMPTY_RESULT;
		
		try {
			
			result = StructrApp.getInstance(securityContext).command(SearchNodeCommand.class).execute(
				Search.andExactTypeAndSubtypes(Principal.class));

		} catch (FrameworkException ex) {
			
			logger.log(Level.WARNING, "Error while searching for principal", ex);

		}

		if (!result.isEmpty()) {

			for (Principal p : result.getResults()) {
				
				userNames.add(p.getProperty(AbstractUser.name));
				
			}
			

		}

		return (String[]) userNames.toArray(new String[userNames.size()]);
		
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
		return (getStructrUser(string) != null);
	}

	@Override
	public User authenticate(Authentication auth) throws AuthenticationFailedException {
		
		logger.log(Level.INFO, "Authentication: {0}", auth);
		String userName = null;
		String password = null;
		
		if (auth instanceof UsernamePasswordAuthentication) {
			
			UsernamePasswordAuthentication authentication = (UsernamePasswordAuthentication) auth;
			
			userName = authentication.getUsername();
			password = authentication.getPassword();
			
			org.structr.web.entity.User user = null;
			try {
				user = (org.structr.web.entity.User) AuthHelper.getPrincipalForPassword(AbstractUser.name, userName, password);
			} catch (AuthenticationException ex) {
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
		return (org.structr.web.entity.User) AuthHelper.getPrincipalForCredential(AbstractUser.name, userName);
	}

}
