/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.auth;

import java.util.logging.Logger;
import org.structr.core.app.StructrApp;

/**
 *
 *
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
		
		return StructrApp.getConfigurationValue("oauth.google.user_details_resource_uri", "");
			
	}

	@Override
	public String getReturnUri() {
		
		return StructrApp.getConfigurationValue("oauth.google.return_uri", "/");
			
	}

	@Override
	public String getErrorUri() {
		
		return StructrApp.getConfigurationValue("oauth.google.error_uri", "/");
			
	}
	
}
