/*
 *  Copyright (C) 2013 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.web.auth;

import java.util.logging.Logger;

/**
 *
 * @author Axel Morgner
 */
public class OpenIdAuthServer extends OAuth2Server {
	
	private static final Logger logger = Logger.getLogger(OpenIdAuthServer.class.getName());
	
	public OpenIdAuthServer(final String authorizationLocation, final String tokenLocation, final String clientId, final String clientSecret, final String redirectUri) {

		super(authorizationLocation, tokenLocation, clientId, clientSecret, redirectUri);
		
	}
	
}
