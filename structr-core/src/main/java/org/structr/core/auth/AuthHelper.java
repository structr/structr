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


package org.structr.core.auth;

import org.apache.commons.codec.digest.DigestUtils;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.BooleanClause.Occur;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Utility class for authentication
 *
 * @author Axel Morgner
 */
public class AuthHelper {

	private static final String STANDARD_ERROR_MSG = "Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!";
	private static final Logger logger             = Logger.getLogger(AuthHelper.class.getName());

	//~--- get methods ----------------------------------------------------

	/**
	 * Find a {@link Principal} for the given credential
	 * 
	 * @param key
	 * @param value
	 * @return 
	 */
	public static Principal getPrincipalForCredential(final PropertyKey key, final String value) {
		
		SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		Principal principal             = null;
		
		Result result = Result.EMPTY_RESULT;
		try {
			
			result = Services.command(securityContext, SearchNodeCommand.class).execute(
				Search.andExactTypeAndSubtypes(Principal.class),
				Search.andExactProperty(securityContext, key, value));

		} catch (FrameworkException ex) {
			
			logger.log(Level.WARNING, "Error while searching for principal", ex);

		}

		if (!result.isEmpty()) {

			principal = (Principal) result.get(0);

		}

		return principal;
	}
	
	/**
	 * Find a {@link Principal} with matching password and given key or name
	 * 
	 * @param key
	 * @param value
	 * @param password
	 * @return
	 * @throws AuthenticationException 
	 */
	public static Principal getPrincipalForPassword(final PropertyKey key, final String value, final String password) throws AuthenticationException {

		String errorMsg = null;
		Principal principal  = null;

		if (Services.getSuperuserUsername().equals(value) && Services.getSuperuserPassword().equals(password)) {

			logger.log(Level.INFO, "############# Authenticated as superadmin! ############");

			principal = new SuperUser();

		} else {

			try {

				SecurityContext securityContext = SecurityContext.getSuperUserInstance();
				SearchNodeCommand searchNode    = Services.command(securityContext, SearchNodeCommand.class);
				List<SearchAttribute> attrs     = new LinkedList<SearchAttribute>();

				attrs.add(Search.andExactTypeAndSubtypes(Principal.class));
				SearchAttributeGroup group = new SearchAttributeGroup(Occur.MUST);
				group.add(Search.orExactProperty(securityContext, key, value));
				group.add(Search.orExactProperty(securityContext, AbstractNode.name, value));
				attrs.add(group);

				Result principals = searchNode.execute(attrs);
				
				if (!principals.isEmpty()) {
					principal = (Principal) principals.get(0);
				}

				if (principal == null) {

					logger.log(Level.INFO, "No principal found for {0} {1}", new Object[]{ key.dbName(), value });

					errorMsg = STANDARD_ERROR_MSG;

				} else {

					if (principal.getProperty(Principal.blocked)) {

						logger.log(Level.INFO, "Principal {0} is blocked", principal);

						errorMsg = STANDARD_ERROR_MSG;

					}

					if (StringUtils.isEmpty(password)) {

						logger.log(Level.INFO, "Empty password for principal {0}", principal);

						errorMsg = "Empty password, should never happen here!";

					} else {

						String salt			= principal.getProperty(Principal.salt);
						String encryptedPasswordValue;
						
						if (salt != null) {
							
							encryptedPasswordValue	= getHash(password, salt);
						} else {
							
							encryptedPasswordValue	= getSimpleHash(password);
							
						}
						
						String pw			= principal.getEncryptedPassword();

						if (pw == null || !encryptedPasswordValue.equals(pw)) {

							logger.log(Level.INFO, "Wrong password for principal {0}", principal);

							errorMsg = STANDARD_ERROR_MSG;

						}
					
					}

				}

			} catch (FrameworkException fex) {

				fex.printStackTrace();

			}

		}

		if (errorMsg != null) {

			throw new AuthenticationException(errorMsg);
		}

		return principal;

	}

	/**
	 * Find a {@link Principal} for the given session id
	 * 
	 * @param sessionId
	 * @return 
	 */
	public static Principal getPrincipalForSessionId(final String sessionId) {

		Principal user                  = null;
		List<SearchAttribute> attrs     = new LinkedList<SearchAttribute>();
		SecurityContext securityContext = SecurityContext.getSuperUserInstance();

		attrs.add(Search.andExactProperty(securityContext, Principal.sessionId, sessionId));
		attrs.add(Search.andExactTypeAndSubtypes(Principal.class));

		try {

			// we need to search with a super user security context here..
			Result results = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(attrs);

			if (!results.isEmpty()) {

				user = (Principal) results.get(0);

				if ((user != null) && sessionId.equals(user.getProperty(Principal.sessionId))) {

					return user;
				}

			}
		} catch (FrameworkException fex) {

			logger.log(Level.WARNING, "Error while executing SearchNodeCommand", fex);

		}

		return user;

	}

	public static String getHash(final String password, final String salt) {
		
		return DigestUtils.sha512Hex(DigestUtils.sha512Hex(password).concat(salt));
		
	}
	
	public static String getSimpleHash(final String password) {
		
		return DigestUtils.sha512Hex(password);
		
	}

}
