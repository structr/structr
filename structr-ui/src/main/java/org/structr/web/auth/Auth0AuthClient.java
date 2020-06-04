/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;

/**
 *
 *
 */
public class Auth0AuthClient extends StructrOAuthClient {

	public Auth0AuthClient() {}

	@Override
	public String getScope() {
		return "openid profile email";
	}

	@Override
	public String getUserResourceUri() {
		return Settings.OAuthAuth0UserDetailsUri.getValue();
	}

	@Override
	public String getReturnUri() {
		return Settings.OAuthAuth0ReturnUri.getValue();
	}

	@Override
	public String getErrorUri() {
		return Settings.OAuthAuth0ErrorUri.getValue();
	}

	@Override
	public void initializeUser(final Principal user) throws FrameworkException {

		// initialize user from user response
		if (userInfo != null) {

			String name = (String)userInfo.get("nickname");

			// fallback 1
			if (StringUtils.isBlank(name)) {
				name = (String)userInfo.get("name");
			}

			// fallback 2
			if (StringUtils.isBlank(name)) {
				name = (String)userInfo.get("email");
			}

			user.setProperty(Principal.name, name);
		}
	}
}
