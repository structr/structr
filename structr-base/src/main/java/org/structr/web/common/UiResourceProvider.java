/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.web.common;

import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.rest.ResourceProvider;
import org.structr.rest.resource.*;
import org.structr.web.resource.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The default resource provider for structr-ui.
 */
public class UiResourceProvider implements ResourceProvider {

	@Override
	public Map<Pattern, Class<? extends Resource>> getResources() {

		Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();

		resourceMap.put(Pattern.compile(Settings.getValidUUIDRegexString()), UuidResource.class);// matches any UUID configured in settings
		resourceMap.put(Pattern.compile("cypher"), CypherQueryResource.class);          // cypher query
		resourceMap.put(Pattern.compile("graphQL"), GraphQLResource.class);             // graphQL query
		resourceMap.put(Pattern.compile("login"), LoginResource.class);                 // login
		resourceMap.put(Pattern.compile("token"), TokenResource.class);                 // token
		resourceMap.put(Pattern.compile("logout"), LogoutResource.class);               // logout
		resourceMap.put(Pattern.compile("registration"), RegistrationResource.class);   // self-registration
		resourceMap.put(Pattern.compile("me"), MeResource.class);                       // me
		resourceMap.put(Pattern.compile("reset-password"), ResetPasswordResource.class);// reset passwor
		resourceMap.put(Pattern.compile("maintenance"), MaintenanceResource.class);     // maintenance
		resourceMap.put(Pattern.compile("in"), RelationshipResource.class);             // incoming relationship
		resourceMap.put(Pattern.compile("out"), RelationshipResource.class);            // outgoing relationship
		resourceMap.put(Pattern.compile("start"), RelationshipNodeResource.class);      // start node
		resourceMap.put(Pattern.compile("end"), RelationshipNodeResource.class);        // end node

		// FIXME: are views needed here?
		resourceMap.put(Pattern.compile("public"), ViewFilterResource.class);                 // public view (default)
		resourceMap.put(Pattern.compile("protected"), ViewFilterResource.class);              // protected view
		resourceMap.put(Pattern.compile("private"), ViewFilterResource.class);                // private view
		resourceMap.put(Pattern.compile("owner"), ViewFilterResource.class);                  // owner view
		resourceMap.put(Pattern.compile("admin"), ViewFilterResource.class);                  // admin view
		resourceMap.put(Pattern.compile("ids"), ViewFilterResource.class);                    // "ids only" view
		resourceMap.put(Pattern.compile(PropertyView.Ui), ViewFilterResource.class);          // ui view
		resourceMap.put(Pattern.compile(PropertyView.Html), ViewFilterResource.class);        // html attributes view
		resourceMap.put(Pattern.compile(PropertyView.Custom), ViewFilterResource.class);      // custom view

		resourceMap.put(Pattern.compile("log"), LogResource.class);                           // log resource
		resourceMap.put(Pattern.compile("resolver"), EntityResolverResource.class);           // resolves [] of UUIDs to complete result

		resourceMap.put(Pattern.compile("[a-zA-Z]+"), MaintenanceParameterResource.class);    // maintenance parameter
		resourceMap.put(Pattern.compile("[0-9]+"), UuidResource.class);                       // this matches the ID resource

		resourceMap.put(Pattern.compile("_schema"), SchemaResource.class);	               // schema information
		resourceMap.put(Pattern.compile("_schemaJson"), SchemaJsonResource.class);             // schema json import and export !needs to be below any type match
		resourceMap.put(Pattern.compile("_env"), EnvResource.class);	                       // environment information
		resourceMap.put(Pattern.compile("_runtimeEventLog"), RuntimeEventLogResource.class);   // runtime events

		resourceMap.put(Pattern.compile("globalSchemaMethods"),    GlobalSchemaMethodsResource.class);
		resourceMap.put(Pattern.compile("[a-z_A-Z][a-z_A-Z0-9]*"), GlobalSchemaMethodResource.class);

		// fallback, match any type
		resourceMap.put(Pattern.compile("[a-z_A-Z][a-z_A-Z0-9]*"), TypeResource.class); // any type match

		return resourceMap;

	}

}
