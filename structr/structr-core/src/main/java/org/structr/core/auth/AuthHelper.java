/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.auth;

import org.apache.commons.codec.digest.DigestUtils;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;
import org.structr.core.node.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class AuthHelper {

	private static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	private static final Logger logger             = Logger.getLogger(AuthHelper.class.getName());

	//~--- get methods ----------------------------------------------------

	public static Principal getUserForUsernameAndPassword(final SecurityContext securityContext, final String userName, final String password) throws AuthenticationException {

		String errorMsg = null;
		Principal user  = null;

		if (Services.getSuperuserUsername().equals(userName) && Services.getSuperuserPassword().equals(password)) {

			logger.log(Level.INFO, "############# Authenticated as superadmin! ############");

			user = new SuperUser();

		} else {

			try {

				;
				
				Command findNode            = Services.command(securityContext, SearchNodeCommand.class);
				List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

				attrs.add(Search.andExactTypeAndSubtypes(Principal.class.getSimpleName()));
//				attrs.add(Search.andExactType("User"));
				attrs.add(Search.andExactName(userName));

				List<Principal> userList = (List<Principal>) findNode.execute(null, false, false, attrs);
				
				if (!userList.isEmpty()) {
					user = userList.get(0);
				}

				if (user == null) {

					logger.log(Level.INFO, "No user found for name {0}", userName);

					errorMsg = STANDARD_ERROR_MSG;

				} else {

					if (user.isBlocked()) {

						logger.log(Level.INFO, "User {0} is blocked", user);

						errorMsg = STANDARD_ERROR_MSG;

					}

					if (password == null) {

						logger.log(Level.INFO, "Password for user {0} is null", user);

						errorMsg = "You should enter a password.";

					}

					String encryptedPasswordValue = DigestUtils.sha512Hex(password);
					String pw                     = user.getEncryptedPassword();

					if (pw == null || !encryptedPasswordValue.equals(pw)) {

						logger.log(Level.INFO, "Wrong password for user {0}", user);

						errorMsg = STANDARD_ERROR_MSG;

					}

				}

			} catch (FrameworkException fex) {

				fex.printStackTrace();

			}

		}

		if (errorMsg != null) {

			throw new AuthenticationException(errorMsg);
		}

		return user;

	}

	public static Principal getUserForToken(final String messageToken) {

		Principal user              = null;
		List<SearchAttribute> attrs = new LinkedList<SearchAttribute>();

		attrs.add(Search.andExactProperty(Principal.Key.sessionId, messageToken));
		attrs.add(Search.andExactType("User"));

		try {

			// we need to search with a super user security context here..
			List<AbstractNode> results = (List<AbstractNode>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(null, false, false, attrs);

			if (!results.isEmpty()) {

				user = (Principal) results.get(0);

				if ((user != null) && messageToken.equals(user.getProperty(Principal.Key.sessionId))) {

					return user;
				}

			}
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Error while executing SearchNodeCommand", fex);

		}

		return user;

	}

}
