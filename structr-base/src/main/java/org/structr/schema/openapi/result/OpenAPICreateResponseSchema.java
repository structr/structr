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
package org.structr.schema.openapi.result;

import org.structr.core.graph.NodeServiceCommand;
import org.structr.schema.openapi.schema.OpenAPIArraySchema;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class OpenAPICreateResponseSchema extends TreeMap<String, Object> {

    public OpenAPICreateResponseSchema() {
        Map<String, Object> propertiesMap = new HashMap<>();

        put("properties", propertiesMap);
        propertiesMap.put("result", new OpenAPIArraySchema("The UUID(s) of the created object(s).", Map.of("type", "string", "example", NodeServiceCommand.getNextUuid())));
    }

}
