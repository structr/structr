/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.auth.legacy;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;


/**
 *
 *
 */
public class FacebookAuthClient extends StructrOAuthClient {

	public FacebookAuthClient() {}

	@Override
	public String getProviderName () {
		return "facebook";
	}

	@Override
	public String getScope() {
		return Settings.OAuthFacebookScope.getValue();
	}

	@Override
	public String getUserResourceUri() {
		return Settings.OAuthFacebookUserDetailsUri.getValue();
	}

	@Override
	public String getReturnUri() {
		return Settings.OAuthFacebookReturnUri.getValue();
	}

	@Override
	public String getErrorUri() {
		return Settings.OAuthFacebookErrorUri.getValue();
	}

	@Override
	public String getCredential(final HttpServletRequest request) {
		return StringUtils.replace(getValue(request, "email"), "\u0040", "@");
	}

	@Override
	protected String getAccessTokenLocationKey() {
		return Settings.OAuthFacebookAccessTokenLocation.getKey();
	}

	@Override
	protected String getAccessTokenLocation() {
		return Settings.OAuthFacebookAccessTokenLocation.getValue("query");
	}
}
