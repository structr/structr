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

import org.structr.api.config.Settings;

/**
 *
 *
 */
public class GoogleAuthClient extends StructrOAuthClient {

	@Override
	public String getProviderName () {
		return "google";
	}

	@Override
	protected String getScope() {
		return Settings.OAuthGoogleScope.getValue();
	}

	@Override
	public String getUserResourceUri() {
		return Settings.OAuthGoogleUserDetailsUri.getValue();
	}

	@Override
	public String getReturnUri() {
		return Settings.OAuthGoogleReturnUri.getValue();
	}

	@Override
	public String getErrorUri() {
		return Settings.OAuthGoogleErrorUri.getValue();
	}

	@Override
	protected String getAccessTokenLocationKey() {
		return Settings.OAuthGoogleAccessTokenLocation.getKey();
	}

	@Override
	protected String getAccessTokenLocation() {
		return Settings.OAuthGoogleAccessTokenLocation.getValue("query");
	}
}
