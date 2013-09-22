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
package org.structr.web.auth;

import java.util.logging.Logger;
import org.structr.core.Services;

/**
 *
 * @author Axel Morgner
 */
public class GoogleAuthClient extends StructrOAuthClient {
	
	private static final Logger logger = Logger.getLogger(GoogleAuthClient.class.getName());
	
	@Override
	protected String getScope() {
		return "email";
	}

	@Override
	public ResponseFormat getResponseFormat() {
		
		return ResponseFormat.json;
		
	}
	@Override
	public String getUserResourceUri() {
		
		return Services.getConfigurationValue("oauth.google.user_details_resource_uri", "");
			
	}

	@Override
	public String getReturnUri() {
		
		return Services.getConfigurationValue("oauth.google.return_uri", "/");
			
	}

	@Override
	public String getErrorUri() {
		
		return Services.getConfigurationValue("oauth.google.error_uri", "/");
			
	}
	
}
