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
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.app.StructrApp;

/**
 *
 *
 */
public class FacebookAuthClient extends StructrOAuthClient {
	
	private static final Logger logger = Logger.getLogger(FacebookAuthClient.class.getName());
	
	public FacebookAuthClient() {}

	@Override
	public String getScope() {
		
		return "email";
		
	}

	@Override
	public ResponseFormat getResponseFormat() {
		
		return ResponseFormat.urlEncoded;
		
	}

	@Override
	public String getUserResourceUri() {
		
		return StructrApp.getConfigurationValue("oauth.facebook.user_details_resource_uri", "");
			
	}

	@Override
	public String getReturnUri() {
		
		return StructrApp.getConfigurationValue("oauth.facebook.return_uri", "/");
			
	}

	@Override
	public String getErrorUri() {
		
		return StructrApp.getConfigurationValue("oauth.facebook.error_uri", "/");
			
	}
	
	@Override
	public String getCredential(final HttpServletRequest request) {
		
		return StringUtils.replace(getValue(request, "email"), "\u0040", "@");
		
	}
	
}
