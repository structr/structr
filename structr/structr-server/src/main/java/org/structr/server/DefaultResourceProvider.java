/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.server;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.structr.rest.ResourceProvider;
import org.structr.rest.resource.*;

/**
 * A default resource provider implementation for structr.
 *
 * @author Christian Morgner
 */
public class DefaultResourceProvider implements ResourceProvider {

	@Override
	public Map<Pattern, Class<? extends Resource>> getResources() {

		Map<Pattern, Class<? extends Resource>> resourceMap = new LinkedHashMap<Pattern, Class<? extends Resource>>();

		resourceMap.put(Pattern.compile("[a-zA-Z0-9]{32}"),	UuidResource.class);			// matches a UUID without dashes

		resourceMap.put(Pattern.compile("cypher"),		CypherQueryResource.class);		// include experimental cypher support

		resourceMap.put(Pattern.compile("maintenance"),		MaintenanceResource.class);		// maintenance

		resourceMap.put(Pattern.compile("in"),			RelationshipResource.class);		// incoming relationship
		resourceMap.put(Pattern.compile("out"),			RelationshipResource.class);		// outgoing relationship
		resourceMap.put(Pattern.compile("start"),		RelationshipNodeResource.class);	// start node
		resourceMap.put(Pattern.compile("end"),			RelationshipNodeResource.class);	// end node

		resourceMap.put(Pattern.compile("public"),		ViewFilterResource.class);		// public view (default)
		resourceMap.put(Pattern.compile("all"),			ViewFilterResource.class);		// all view

		resourceMap.put(Pattern.compile("[a-zA-Z]+"),		MaintenanceParameterResource.class);	// maintenance parameter

		resourceMap.put(Pattern.compile("_schema"),		SchemaResource.class);			// special resource for schema information
		resourceMap.put(Pattern.compile("[a-z_]+"),		TypeResource.class);			// any type match
		
		return resourceMap;
	}
}
