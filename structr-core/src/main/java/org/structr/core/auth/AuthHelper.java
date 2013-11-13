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

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.auth.exception.AuthenticationException;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SuperUser;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractUser;
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
		
		final App app = StructrApp.getInstance();
		final Query<Principal> query = app.nodeQuery(Principal.class).and(key, value);

		try {
			return query.getFirst();
			
		} catch (FrameworkException fex) {
			
			logger.log(Level.WARNING, "Error while searching for principal", fex);
		}
		
		return null;
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
	public static Principal getPrincipalForPassword(final PropertyKey<String> key, final String value, final String password) throws AuthenticationException {

		String errorMsg = null;
		Principal principal  = null;

		if (Services.getSuperuserUsername().equals(value) && Services.getSuperuserPassword().equals(password)) {

			logger.log(Level.INFO, "############# Authenticated as superadmin! ############");

			principal = new SuperUser();

		} else {

			try {
				
				principal = StructrApp.getInstance().nodeQuery(Principal.class).and().or(key, value).or(AbstractUser.name, value).getFirst();
				
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
		return getPrincipalForCredential(Principal.sessionId, sessionId);
	}

	public static String getHash(final String password, final String salt) {
		
		if (StringUtils.isEmpty(salt)) {
			
			return getSimpleHash(password);
			
		}
		
		return DigestUtils.sha512Hex(DigestUtils.sha512Hex(password).concat(salt));
		
	}
	
	public static String getSimpleHash(final String password) {
		
		return DigestUtils.sha512Hex(password);
		
	}

}
