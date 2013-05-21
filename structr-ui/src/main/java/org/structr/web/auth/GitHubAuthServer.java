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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.structr.core.Services;

/**
 *
 * @author Axel Morgner
 */
public class GitHubAuthServer extends OAuth2Server {
	
	private static final Logger logger = Logger.getLogger(GitHubAuthServer.class.getName());

	public GitHubAuthServer() {};

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
		
		return Services.getConfigurationValue("oauth.github.user_details_resource_uri", "");
			
	}

	@Override
	public String getReturnUri() {
		
		return Services.getConfigurationValue("oauth.github.return_uri", "/");
			
	}

	@Override
	public String getErrorUri() {
		
		return Services.getConfigurationValue("oauth.github.error_uri", "/");
			
	}

	@Override
	public String getEmail(final HttpServletRequest request) {
		
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
