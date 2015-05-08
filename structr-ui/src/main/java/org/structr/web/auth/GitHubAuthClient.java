/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.structr.core.app.StructrApp;

/**
 *
 * @author Axel Morgner
 */
public class GitHubAuthClient extends StructrOAuthClient {
	
	private static final Logger logger = Logger.getLogger(GitHubAuthClient.class.getName());

	public GitHubAuthClient() {};

	@Override
	public String getScope() {
		
		return "user:email";
		
	}
	
	@Override
	public ResponseFormat getResponseFormat() {
		
		return ResponseFormat.urlEncoded;
		
	}
	
	@Override
	public String getUserResourceUri() {
		
		return StructrApp.getConfigurationValue("oauth.github.user_details_resource_uri", "");
			
	}

	@Override
	public String getReturnUri() {
		
		return StructrApp.getConfigurationValue("oauth.github.return_uri", "/");
			
	}

	@Override
	public String getErrorUri() {
		
		return StructrApp.getConfigurationValue("oauth.github.error_uri", "/");
			
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
