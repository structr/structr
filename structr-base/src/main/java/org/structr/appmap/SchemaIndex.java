/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.appmap;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Setting;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the map needs to know about the schema to resolve scripts the way Structr does: the
 * types and their parents, the properties of every type including its relationship ends
 * (under their JSON names), where a relationship name leads, and the methods by owner and
 * name.
 */
public class SchemaIndex {

	// relationships that Structr brings along, as source type, property name and target type
	private static final String[][] BUILTIN_RELATIONSHIPS = {
		{ "User",      "groups",  "Group"  },
		{ "Group",     "members", "User"   },
		{ "Principal", "groups",  "Group"  },
		{ "Folder",    "files",   "File"   },
		{ "Folder",    "folders", "Folder" },
		{ "File",      "parent",  "Folder" }
	};

	private static final Pattern LIFECYCLE = Pattern.compile("^(on|after)(Create|Save|Delete|Modification|Upload|Download|NodeCreation)");

	private final Map<String, String> typeIds               = new LinkedHashMap<>();
	private final Map<String, List<String>> parents         = new LinkedHashMap<>();
	private final Map<String, Set<String>> properties       = new LinkedHashMap<>();
	private final Map<String, Set<String>> typesByProperty  = new LinkedHashMap<>();
	private final Map<String, String> relationshipTargets   = new LinkedHashMap<>();
	private final Map<String, Method> methods               = new LinkedHashMap<>();
	private final Map<String, String> globalMethods         = new LinkedHashMap<>();
	private final Map<String, String> typeMethods           = new LinkedHashMap<>();
	private final Map<String, Map<String, Double>> methodTypes = new LinkedHashMap<>();

	SchemaIndex(final App app) throws FrameworkException {

		for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_NODE).getAsList()) {

			final SchemaNode schemaNode = node.as(SchemaNode.class);
			final String name           = schemaNode.getName();

			typeIds.put(name, schemaNode.getUuid());
			parents.put(name, new LinkedList<>(schemaNode.getInheritedTraits()));
		}

		final Traits propertyTraits = Traits.of(StructrTraits.SCHEMA_PROPERTY);

		for (final NodeInterface property : app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).getAsList()) {

			final NodeInterface owner = property.getProperty(propertyTraits.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY));
			final Boolean builtin     = property.getProperty(propertyTraits.key(SchemaPropertyTraitDefinition.IS_BUILTIN_PROPERTY_PROPERTY));

			if (owner != null && !Boolean.TRUE.equals(builtin)) {
				propertiesOf(owner.getName()).add(property.getName());
			}
		}

		// the JSON name on the source side leads to the target, and the other way round
		for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_RELATIONSHIP_NODE).getAsList()) {

			final SchemaRelationshipNode rel = node.as(SchemaRelationshipNode.class);
			final String source              = sourceName(rel);
			final String target              = targetName(rel);

			if (source != null && target != null) {

				if (rel.getTargetJsonName() != null) {

					relationshipTargets.put(source + "." + rel.getTargetJsonName(), target);
					propertiesOf(source).add(rel.getTargetJsonName());
				}

				if (rel.getSourceJsonName() != null) {

					relationshipTargets.put(target + "." + rel.getSourceJsonName(), source);
					propertiesOf(target).add(rel.getSourceJsonName());
				}
			}
		}

		for (final String[] rel : BUILTIN_RELATIONSHIPS) {

			relationshipTargets.putIfAbsent(rel[0] + "." + rel[1], rel[2]);
			propertiesOf(rel[0]).add(rel[1]);
		}

		// a subtype has the properties of its parents
		final Set<String> allTypes = new LinkedHashSet<>(properties.keySet());

		allTypes.addAll(typeIds.keySet());

		for (final String type : allTypes) {

			for (final String property : allPropertiesOf(type, new HashSet<>())) {
				typesByProperty.computeIfAbsent(property, k -> new LinkedHashSet<>()).add(type);
			}
		}

		for (final NodeInterface node : app.nodeQuery(StructrTraits.SCHEMA_METHOD).getAsList()) {

			final SchemaMethod schemaMethod = node.as(SchemaMethod.class);
			final AbstractSchemaNode owner  = schemaMethod.getSchemaNode();
			final boolean ownedByType       = owner != null && owner.is(StructrTraits.SCHEMA_NODE);
			final Method method             = new Method(schemaMethod.getUuid(), schemaMethod.getName(), owner != null ? owner.getName() : null, ownedByType ? owner.getUuid() : null, schemaMethod.getSource());

			methods.put(method.id, method);

			if (owner == null) {
				globalMethods.put(method.name, method.id);
			} else {
				typeMethods.put(method.owner + "." + method.name, method.id);
			}
		}

		// the types a method deals with: those its script names, and its owner most
		for (final Method method : methods.values()) {

			final Map<String, Double> weights = new LinkedHashMap<>();
			final Map<String, Integer> counts = new LinkedHashMap<>();

			if (method.source != null) {

				final Matcher matcher = ScriptScanner.TYPE_CALL.matcher(method.source);

				while (matcher.find()) {

					if (typeIds.containsKey(matcher.group(1))) {
						counts.merge(matcher.group(1), 1, Integer::sum);
					}
				}
			}

			counts.forEach((type, count) -> weights.merge(type, 1 + Math.log(count), Double::sum));

			if (method.ownerTypeId != null) {
				weights.merge(method.owner, 1.5 + 3, Double::sum);
			}

			methodTypes.put(method.id, weights);
		}
	}

	// ----- public methods -----
	public Map<String, String> getTypeIds() {

		return typeIds;
	}

	public boolean isType(final String name) {

		return name != null && typeIds.containsKey(name);
	}

	public List<String> getParents(final String type) {

		return parents.getOrDefault(type, List.of());
	}

	/**
	 * The types that have a property of this name, directly or inherited.
	 */
	public Set<String> getTypesWithProperty(final String property) {

		return typesByProperty.getOrDefault(property, Set.of());
	}

	/**
	 * Where the relationship property of this name leads from a type, or null.
	 */
	public String getRelationshipTarget(final String type, final String property) {

		return relationshipTargets.get(type + "." + property);
	}

	/**
	 * The number of properties a type declares itself, without those it inherits.
	 */
	public int getPropertyCount(final String type) {

		return properties.getOrDefault(type, Set.of()).size();
	}

	public Collection<Method> getMethods() {

		return methods.values();
	}

	public Method getMethod(final String id) {

		return methods.get(id);
	}

	public String getGlobalMethod(final String name) {

		return globalMethods.get(name);
	}

	public String getTypeMethod(final String type, final String name) {

		return typeMethods.get(type + "." + name);
	}

	/**
	 * The type a data key is named after, ignoring case, e.g. creditnote for CreditNote, or null.
	 */
	public String getTypeNamedLike(final String key) {

		for (final String type : typeIds.keySet()) {

			if (type.equalsIgnoreCase(key)) {
				return type;
			}
		}

		return null;
	}

	/**
	 * The id of the method of this name if exactly one type has one, or null.
	 */
	public String getOnlyTypeMethod(final String name) {

		final List<Method> found = methods.values().stream().filter(method -> method.ownerTypeId != null && method.name.equals(name)).toList();

		return found.size() == 1 ? found.get(0).id : null;
	}

	/**
	 * The types a method deals with, strongest first.
	 */
	public List<String> getMethodTypes(final String methodId) {

		final Map<String, Double> weights = methodTypes.getOrDefault(methodId, Map.of());
		final List<String> types          = new ArrayList<>(weights.keySet());

		types.sort((a, b) -> Double.compare(weights.get(b), weights.get(a)));

		return types;
	}

	// ----- private methods -----
	private Set<String> propertiesOf(final String type) {

		return properties.computeIfAbsent(type, k -> new LinkedHashSet<>());
	}

	private Set<String> allPropertiesOf(final String type, final Set<String> seen) {

		final Set<String> result = new LinkedHashSet<>(properties.getOrDefault(type, Set.of()));

		seen.add(type);

		for (final String parent : getParents(type)) {

			if (!seen.contains(parent)) {
				result.addAll(allPropertiesOf(parent, seen));
			}
		}

		return result;
	}

	private String sourceName(final SchemaRelationshipNode rel) {

		final SchemaNode source = rel.getSourceNode();

		return source != null ? source.getName() : rel.getSourceType();
	}

	private String targetName(final SchemaRelationshipNode rel) {

		final SchemaNode target = rel.getTargetNode();

		return target != null ? target.getName() : rel.getTargetType();
	}

	// ----- nested classes -----
	/**
	 * A schema method: its owner is the name of its schema node (a type or a relationship), its
	 * owner type the id of the type it belongs to, if it belongs to one.
	 */
	public static class Method {

		public final String id;
		public final String name;
		public final String owner;
		public final String ownerTypeId;
		public final String source;
		public final boolean isLifecycle;
		public final String cronExpression;

		Method(final String id, final String name, final String owner, final String ownerTypeId, final String source) {

			this.id             = id;
			this.name           = name;
			this.owner          = owner;
			this.ownerTypeId    = ownerTypeId;
			this.source         = source != null ? source : "";
			this.isLifecycle    = name != null && LIFECYCLE.matcher(name).find();
			this.cronExpression = cronExpressionOf(getLabel());
		}

		/**
		 * The name on the map: owner and name, or the name alone for a global method.
		 */
		public String getLabel() {

			return owner != null ? owner + "." + name : name;
		}

		/**
		 * The schedule of a method that CronService runs: a user-defined function by its name, a
		 * static method as Type.method, each listed in CronService.tasks with its own
		 * &lt;task&gt;.cronExpression.
		 */
		private static String cronExpressionOf(final String label) {

			final String tasks = Settings.CronTasks.getValue();

			if (StringUtils.isBlank(tasks) || label == null) {
				return null;
			}

			for (final String task : tasks.split("[ \\t]+")) {

				if (label.equals(task)) {

					final Setting<?> expression = Settings.getCaseSensitiveSetting(task, "cronExpression");

					return expression != null && expression.getValue() != null ? expression.getValue().toString() : null;
				}
			}

			return null;
		}
	}
}
