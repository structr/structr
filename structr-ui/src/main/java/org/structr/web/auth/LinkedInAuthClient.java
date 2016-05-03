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

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.structr.core.app.StructrApp;

/**
 *
 *
 */
public class LinkedInAuthClient extends StructrOAuthClient {
	
	private static final Logger logger = Logger.getLogger(LinkedInAuthClient.class.getName());

	public LinkedInAuthClient() {};

	@Override
	protected String getAccessTokenParameterKey() {
		return "oauth2_access_token";
	}

	@Override
	public String getScope() {
		
		return "r_basicprofile r_emailaddress";
		
	}
	
	@Override
	public ResponseFormat getResponseFormat() {
		
		return ResponseFormat.json;
		
	}
	
	@Override
	public String getUserResourceUri() {
		
		return StructrApp.getConfigurationValue("oauth.linkedin.user_details_resource_uri", "");
			
	}

	@Override
	public String getReturnUri() {
		
		return StructrApp.getConfigurationValue("oauth.linkedin.return_uri", "/");
			
	}

	@Override
	public String getErrorUri() {
		
		return StructrApp.getConfigurationValue("oauth.linkedin.error_uri", "/");
			
	}

	@Override
	protected String getState() {
		
		return UUID.randomUUID().toString();
		
	}

	@Override
	public String getCredential(final HttpServletRequest request) {
		
		OAuthResourceResponse userResponse = getUserResponse(request);
		
		if (userResponse == null) {
			
			return null;
			
		}
		
		String body = userResponse.getBody();
		logger.log(Level.FINE, "User response body: {0}", body);
		
		String[] addresses = StringUtils.stripAll(StringUtils.stripAll(StringUtils.stripEnd(StringUtils.stripStart(body, "["), "]").split(",")), "\"");

		return addresses.length > 0 ? addresses[0] : null;
		
	}

}
