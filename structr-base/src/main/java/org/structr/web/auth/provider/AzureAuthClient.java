/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.auth.provider;

import com.github.scribejava.apis.MicrosoftAzureActiveDirectory20Api;
import jakarta.servlet.http.HttpServletRequest;
import org.structr.api.config.Settings;
import org.structr.web.auth.AbstractOAuth2Client;
import org.structr.web.auth.OAuth2ProviderRegistry;

/**
 * Azure Active Directory OAuth2 client implementation.
 *
 * Configuration:
 *    oauth.azure.tenant_id = your-tenant-id (or "common" for multi-tenant)
 *    oauth.azure.user_details_resource_uri = user details endpoint (required)
 *
 * ScribeJava's MicrosoftAzureActiveDirectory20Api automatically handles
 * authorization and token endpoint URL construction based on the tenant ID.
 */
public class AzureAuthClient extends AbstractOAuth2Client {

	private static final String AUTH_SERVER = "azure";

	public AzureAuthClient(final HttpServletRequest request, OAuth2ProviderRegistry.ProviderConfig providerConfig) {

		final String tenantId = Settings.OAuthAzureTenantId.getValue();

		super(request, AUTH_SERVER, MicrosoftAzureActiveDirectory20Api.custom(tenantId), providerConfig);
	}
}