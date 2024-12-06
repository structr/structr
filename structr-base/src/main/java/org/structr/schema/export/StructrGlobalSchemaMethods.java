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
package org.structr.schema.export;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaMethodParameter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;

import java.util.*;

public class StructrGlobalSchemaMethods {

	private List<Map<String, Object>> globalMethods = new LinkedList<>();

	public Map<String, Object> serializeOpenAPIOperations(final String tag) {

		final App app                     = StructrApp.getInstance();
		final Map<String, Object> methods = new TreeMap<>();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface schemaMethod : app.nodeQuery("SchemaMethod").and(SchemaMethod.schemaNode, null).sort(SchemaMethod.name).getAsList()) {

				final StructrMethodDefinition def = StructrMethodDefinition.deserialize(null, schemaMethod);

				// filter by tag
				if (StringUtils.isBlank(tag) || def.getTags().contains(tag)) {

					methods.putAll(def.serializeOpenAPI(null, null));
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

		}

		return methods;
	}

	void deserialize(final App app) throws FrameworkException {

		final Traits traits                            = Traits.of("SchemaMethod");
		final PropertyKey<NodeInterface> schemaNodeKey = traits.key("schemaNode");
		final PropertyKey<NodeInterface> nameKey       = traits.key("name");

		try (final Tx tx = app.tx()) {

			for (final NodeInterface node : app.nodeQuery("SchemaMethod").and(schemaNodeKey, null).sort(nameKey).getAsList()) {

				final Map<String, Object> entry  = new TreeMap<>();
				final Map<String, Object> params = new LinkedHashMap<>();

				globalMethods.add(entry);

				final SchemaMethod schemaMethod = node.as(SchemaMethod.class);

				entry.put(JsonSchema.KEY_NAME,                schemaMethod.getName());
				entry.put(JsonSchema.KEY_SOURCE,              schemaMethod.getSource());
				entry.put(JsonSchema.KEY_TAGS,                schemaMethod.getTags());
				entry.put(JsonSchema.KEY_INCLUDE_IN_OPENAPI,  schemaMethod.includeInOpenAPI());
				entry.put(JsonSchema.KEY_OPENAPI_RETURN_TYPE, schemaMethod.getOpenAPIReturnType());
				entry.put(JsonSchema.KEY_SUMMARY,             schemaMethod.getSummary());
				entry.put(JsonSchema.KEY_DESCRIPTION,         schemaMethod.getDescription());
				entry.put(JsonSchema.KEY_IS_PRIVATE,          schemaMethod.isPrivateMethod());
				entry.put(JsonSchema.KEY_RETURN_RAW_RESULT,   schemaMethod.returnRawResult());
				entry.put(JsonSchema.KEY_HTTP_VERB,           schemaMethod.getHttpVerb());

				// TODO: remove
				entry.put("virtualFileName",             schemaMethod.getVirtualFileName());
				entry.put("visibleToAuthenticatedUsers", schemaMethod.visibleToAuthenticatedUsers());
				entry.put("visibleToPublicUsers",        schemaMethod.visibleToPublicUsers());

				for (final NodeInterface parameterNode : schemaMethod.getParameters()) {

					final StructrParameterDefinition def = new StructrParameterDefinition(null, schemaMethod.getName());
					final SchemaMethodParameter param    = parameterNode.as(SchemaMethodParameter.class);

					def.setType(param.getParameterType());
					def.setIndex(param.getIndex());
					def.setDescription(param.getDescription());
					def.setExampleValue(param.getExampleValue());

					params.put(param.getName(), def.serialize());
				}

				entry.put(JsonSchema.KEY_PARAMETERS, params);
			}

			tx.success();
		}
	}

	List<Map<String, Object>> serialize() {

		return globalMethods;
	}

	void deserialize(final List<Map<String, Object>> source) {

		globalMethods = source;
	}

	public void createDatabaseSchema(final App app, final JsonSchema.ImportMode importMode) throws FrameworkException {

		final PropertyKey<NodeInterface> schemaNodeKey = Traits.of("SchemaMethod").key("schemaNode");
		final SecurityContext context                  = SecurityContext.getSuperUserInstance();

		context.setDoTransactionNotifications(false);

		if (JsonSchema.ImportMode.replace.equals(importMode)) {
			// completely delete all global schema methods and import the methods from file

			for (final NodeInterface method : app.nodeQuery("SchemaMethod").and(schemaNodeKey, null).getAsList()) {
				app.delete(method);
			}

			for (final Map<String, Object> entry : globalMethods) {

				createMethod(app, context, entry);
			}

		} else if (JsonSchema.ImportMode.extend.equals(importMode)) {
			// import the methods from file and delete pre-existing global schema methods present in the file
			// Note: this can only happen if a complete snapshot is used to extend another database

			for (final Map<String, Object> entry : globalMethods) {

				final String name = entry.get(JsonSchema.KEY_NAME).toString();

				for (final NodeInterface method : app.nodeQuery("SchemaMethod").and(schemaNodeKey, null).andName(name).getAsList()) {
					app.delete(method);
				}

				createMethod(app, context, entry);
			}
		}
	}

	void createMethod(final App app, final SecurityContext context, Map<String, Object> entry) throws FrameworkException {

		final Map<String, Map<String, Object>> params;
		if (entry.containsKey(JsonSchema.KEY_PARAMETERS)) {
			params = (Map)entry.remove(JsonSchema.KEY_PARAMETERS);
		} else {
			params = Map.of();
		}

		final NodeInterface method = app.create("SchemaMethod", PropertyMap.inputTypeToJavaType(context, "SchemaMethod", entry));

		for (final Map.Entry<String, Map<String, Object>> paramEntry : params.entrySet()) {

			StructrParameterDefinition pDef = new StructrParameterDefinition(null, paramEntry.getKey());
			pDef.deserialize(paramEntry.getValue());

			pDef.createDatabaseSchema(app, method, pDef.getIndex());
		}
	}

	public void clear() {
		globalMethods.clear();
	}

	// ----- private methods -----
	private Set<String> getTags(final Map<String, Object> method) {

		final Object tags = method.get("tags");
		if (tags != null) {

			if (tags instanceof Collection) {

				return new LinkedHashSet<>((Collection)tags);
			}

			if (tags.getClass().isArray()) {

				return new LinkedHashSet<>(Arrays.asList((String[])tags));
			}
		}

		return Set.of();
	}

	private boolean isSelected(final Set<String> tags, final String tag) {

		boolean selected = tag == null || tags.contains(tag);

		// don't show types without tags
		if (tags.isEmpty()) {
			return false;
		}

		// skip blacklisted tags
		if (intersects(StructrTypeDefinition.TagBlacklist, tags)) {

			// if a tag is selected, it overrides the blacklist
			selected = tag != null && tags.contains(tag);
		}

		return selected;
	}

	private boolean intersects(final Set<String> set1, final Set<String> set2) {

		final Set<String> intersection = new LinkedHashSet<>(set1);

		intersection.retainAll(set2);

		return !intersection.isEmpty();
	}

}
