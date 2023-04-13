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
package org.structr.rest;

import org.structr.api.config.Settings;
import org.structr.rest.resource.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A default resource provider implementation for structr.
 *
 *
 */
public class DefaultResourceProvider implements ResourceProvider {

	@Override
	public Map<Pattern, Class<? extends Resource>> getResources() {

		Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<>();

		resourceMap.put(Pattern.compile(Settings.UuidPattern.getValue()), UuidResource.class); // matches any UUID configured in settings
		resourceMap.put(Pattern.compile("cypher"),        CypherQueryResource.class);          // include experimental cypher support

		resourceMap.put(Pattern.compile("maintenance"),   MaintenanceResource.class);          // maintenance

		resourceMap.put(Pattern.compile("in"),            RelationshipResource.class);         // incoming relationship
		resourceMap.put(Pattern.compile("out"),           RelationshipResource.class);         // outgoing relationship
		resourceMap.put(Pattern.compile("start"),         RelationshipNodeResource.class);     // start node
		resourceMap.put(Pattern.compile("end"),			RelationshipNodeResource.class);     // end node

		resourceMap.put(Pattern.compile("public"),        ViewFilterResource.class);           // public view (default)

		resourceMap.put(Pattern.compile("log"),           LogResource.class);                  // log resource
		resourceMap.put(Pattern.compile("resolver"),      EntityResolverResource.class);       // resolves [] of UUIDs to complete result

		resourceMap.put(Pattern.compile("[a-zA-Z]+"),     MaintenanceParameterResource.class); // maintenance parameter

		// needed to make global schema method execution via maintenance resource testable..
		resourceMap.put(Pattern.compile("globalSchemaMethods"),    GlobalSchemaMethodsResource.class);
		resourceMap.put(Pattern.compile("[a-z_A-Z][a-z_A-Z0-9]*"), GlobalSchemaMethodResource.class);

		resourceMap.put(Pattern.compile("_schema"),                SchemaResource.class);            // special resource for schema information
		resourceMap.put(Pattern.compile("_schemaJson"),            SchemaJsonResource.class);  // special resource for schema json import and export !needs to be below any type match
		resourceMap.put(Pattern.compile("[a-z_A-Z][a-z_A-Z0-9]*"), TypeResource.class);        // any type match


		return resourceMap;
	}
}
