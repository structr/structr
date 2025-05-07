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
package org.structr.core.traits;

import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.ScriptMethod;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.accesscontrollable.AllowedBySchema;

import java.util.*;
import java.util.stream.Collectors;

public class Trait {

	private static final Set<String> DEFAULT_PROPERTY_KEYS = new LinkedHashSet<>(Arrays.asList("id", "type", "name"));

	private final Map<Class, NodeTraitFactory> nodeTraitFactories                 = new HashMap<>();
	private final Map<Class, RelationshipTraitFactory> relationshipTraitFactories = new HashMap<>();
	private final Map<String, AbstractMethod> dynamicMethods                      = new LinkedHashMap<>();
	private final Map<Class, FrameworkMethod> frameworkMethods                    = new LinkedHashMap<>();
	private final Map<Class, LifecycleMethod> lifecycleMethods                    = new LinkedHashMap<>();
	private final Map<String, PropertyKey> propertyKeys                           = new LinkedHashMap<>();
	private final Map<String, Set<String>> views                                  = new LinkedHashMap<>();

	private final Relation relation;
	private final String label;
	private final String name;
	private final boolean isRelationship;
	private final boolean isDynamic;

	public Trait(final TraitDefinition traitDefinition, final boolean isDynamic) {

		this.label          = traitDefinition.getLabel();
		this.name           = traitDefinition.getName();
		this.isRelationship = traitDefinition.isRelationship();
		this.isDynamic      = isDynamic;
		this.relation       = traitDefinition.getRelation();

		initializeFrom(traitDefinition);
	}

	public final void initializeFrom(final TraitDefinition traitDefinition) {

		// properties need to be registered first so they are available in lifecycle methods etc.
		for (final PropertyKey key : traitDefinition.getPropertyKeys()) {

			registerPropertyKey(key);
		}

		lifecycleMethods.putAll(traitDefinition.getLifecycleMethods());
		frameworkMethods.putAll(traitDefinition.getFrameworkMethods());

		// dynamic methods
		for (final AbstractMethod method : traitDefinition.getDynamicMethods()) {
			this.dynamicMethods.put(method.getName(), method);
		}

		// views
		for (final Map.Entry<String, Set<String>> views : traitDefinition.getViews().entrySet()) {

			final Set<String> set = this.views.computeIfAbsent(views.getKey(), k -> new LinkedHashSet<>());

			set.addAll(views.getValue());
		}


		// trait implementations
		this.nodeTraitFactories.putAll(traitDefinition.getNodeTraitFactories());
		this.relationshipTraitFactories.putAll(traitDefinition.getRelationshipTraitFactories());
	}

	@Override
	public String toString() {
		return "Trait(" + name + ")";
	}

	public String getName() {
		return name;
	}
	public String getLabel() {
		return label;
	}

	public Set<String> getPropertyKeysForView(final String viewName) {
		return views.get(viewName);
	}

	public <T extends LifecycleMethod> T getLifecycleMethod(final Class<T> type) {
		return (T) lifecycleMethods.get(type);
	}

	public <T extends FrameworkMethod> T getFrameworkMethod(final Class<T> type) {
		return (T) frameworkMethods.get(type);
	}

	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return relationshipTraitFactories;
	}

	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return nodeTraitFactories;
	}

	public Map<String, AbstractMethod> getDynamicMethods() {
		return dynamicMethods;
	}

	public Set<String> getViewNames() {
		return views.keySet();
	}

	public Map<String, Set<String>> getViews() {
		return views;
	}

	public Map<String, PropertyKey> getPropertyKeys() {
		return propertyKeys;
	}

	public Relation getRelation() {
		return relation;
	}

	public boolean isRelationship() {
		return isRelationship;
	}

	public void registerDynamicMethod(final SchemaMethod method) {

		if (method.isLifecycleMethod()) {

			final Class<LifecycleMethod> type = method.getMethodType();
			if (type != null) {

				lifecycleMethods.put(type, method.asLifecycleMethod());

			} else {

				// report error?
			}

		} else {

			dynamicMethods.put(method.getName(), new ScriptMethod(method));
		}
	}

	public boolean isDynamic() {
		return isDynamic;
	}

	public void registerDynamicProperty(final SchemaProperty schemaProperty) throws FrameworkException {

		final PropertyKey key = schemaProperty.createKey(getLabel());
		if (key != null) {

			registerPropertyKey(key);
		}
	}

	public void registerDynamicView(final SchemaView schemaView) {

		final String name      = schemaView.getName();
		final Set<String> view = views.computeIfAbsent(name, k -> new LinkedHashSet<>());

		for (final SchemaProperty property : schemaView.getSchemaProperties()) {

			view.add(property.getName());
		}
	}

	public void registerSchemaGrant(final SchemaGrant grant) {

		final String principalId                   = grant.getPrincipal().getUuid();
		final Set<String> readPermissions          = new LinkedHashSet<>();
		final Set<String> writePermissions         = new LinkedHashSet<>();
		final Set<String> deletePermissions        = new LinkedHashSet<>();
		final Set<String> accessControlPermissions = new LinkedHashSet<>();
		boolean hasGrants                          = false;

		if (grant.allowRead()) {
			readPermissions.add(principalId);
		}

		if (grant.allowWrite()) {
			writePermissions.add(principalId);
		}

		if (grant.allowDelete()) {
			deletePermissions.add(principalId);
		}

		if (grant.allowAccessControl()) {
			accessControlPermissions.add(principalId);
		}

		hasGrants = true;

		if (hasGrants) {

			frameworkMethods.put(AllowedBySchema.class, new AllowedBySchema() {

				@Override
				public boolean allowedBySchema(final NodeInterface node, final Principal principal, final Permission permission) {

					final Set<String> ids = principal.getOwnAndRecursiveParentsUuids();

					switch (permission.name()) {

						case "read":          return !org.apache.commons.collections4.SetUtils.intersection(readPermissions, ids).isEmpty();
						case "write":         return !org.apache.commons.collections4.SetUtils.intersection(writePermissions, ids).isEmpty();
						case "delete":        return !org.apache.commons.collections4.SetUtils.intersection(deletePermissions, ids).isEmpty();
						case "accessControl": return !org.apache.commons.collections4.SetUtils.intersection(accessControlPermissions, ids).isEmpty();
					}

					return getSuper().allowedBySchema(node, principal, permission);
				}
			});
		}
	}

	public void registerPropertyKey(final PropertyKey key) {

		final String name = key.jsonName();

		// register property key
		propertyKeys.put(name, key);

		// set declaring trait
		key.setDeclaringTrait(this);

		if (!key.serializationDisabled()) {

			// add key to "all" view
			this.views.computeIfAbsent("all", k -> new LinkedHashSet<>()).add(name);
		}

		// add dynamic keys to "custom" view
		if (key.isDynamic() || DEFAULT_PROPERTY_KEYS.contains(name)) {
			this.views.computeIfAbsent("custom", k -> new LinkedHashSet<>()).add(name);
		}
	}

	public void checkCompatibilityWith(final Trait otherTrait) throws FrameworkException {

		if (!label.equals(otherTrait.label)) {

			final Set<PropertyKey> propertyKeyIntersection = new HashSet<>();
			propertyKeyIntersection.addAll(propertyKeys.values());
			propertyKeyIntersection.retainAll(otherTrait.propertyKeys.values());

			if (!propertyKeyIntersection.isEmpty()){

				throw new FrameworkException(422, "Incompatible traits: trait " + name + " clashes with trait " + otherTrait.name + " because both define the same properties " + propertyKeyIntersection);
			}

			final Set<FrameworkMethod> frameworkMethodIntersection = new HashSet<>();
			frameworkMethodIntersection.addAll(frameworkMethods.values());
			frameworkMethodIntersection.retainAll(otherTrait.frameworkMethods.values());

			if (!frameworkMethodIntersection.isEmpty()){

				throw new FrameworkException(422, "Incompatible traits: trait " + name + " clashes with trait " + otherTrait.name + " because both define the same methods " + frameworkMethodIntersection.stream().map(m -> m.getClass().getSuperclass().getName()).collect(Collectors.toList()));
			}

			final Set<AbstractMethod> dynamicMethodIntersection = new HashSet<>();
			dynamicMethodIntersection.addAll(dynamicMethods.values());
			dynamicMethodIntersection.retainAll(otherTrait.dynamicMethods.values());

			if (!dynamicMethodIntersection.isEmpty()){

				throw new FrameworkException(422, "Incompatible traits: trait " + name + " clashes with trait " + otherTrait.name + " because both define the same methods " + dynamicMethodIntersection.stream().map(m -> m.getFullMethodName()).collect(Collectors.toList()));
			}
		}
	}
}
