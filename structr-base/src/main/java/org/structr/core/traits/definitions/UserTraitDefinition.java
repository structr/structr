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
package org.structr.core.traits.definitions;

import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.relationship.ImagePICTURE_OFUser;
import org.structr.web.entity.relationship.UserHOME_DIRFolder;
import org.structr.web.entity.relationship.UserWORKING_DIRFolder;

import java.util.Map;
import java.util.Set;

public final class UserTraitDefinition extends AbstractTraitDefinition {

	private static final Property<NodeInterface> homeDirectoryProperty       = new EndNode("homeDirectory", "UserHOME_DIRFolder").partOfBuiltInSchema();
	private static final Property<NodeInterface> workingDirectoryProperty    = new EndNode("workingDirectory", "UserWORKING_DIRFolder").partOfBuiltInSchema();
	private static final Property<NodeInterface> imgProperty                 = new StartNode("img", "ImagePICTURE_OFUser");
	private static final Property<String> confirmationKeyProperty            = new StringProperty("confirmationKey").indexed().partOfBuiltInSchema();
	private static final Property<String> localStorageProperty               = new StringProperty("localStorage").partOfBuiltInSchema();
	private static final Property<Boolean> skipSecurityRelationshipsProperty = new BooleanProperty("skipSecurityRelationships").defaultValue(false).indexed().partOfBuiltInSchema();
	private static final Property<Boolean> isUserProperty                    = new ConstantBooleanProperty("isUser", true).partOfBuiltInSchema();

	public UserTraitDefinition() {
		super("User");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {
		return Map.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of(
			homeDirectoryProperty,
			workingDirectoryProperty,
			imgProperty,
			confirmationKeyProperty,
			localStorageProperty,
			skipSecurityRelationshipsProperty,
			isUserProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	/*
	@Override
	public Folder getHomeDirectory() {
		return getProperty(traits.key("homeDirectory"));
	}

	@Override
	public void setWorkingDirectory(final Folder workDir) throws FrameworkException {
		setProperty(traits.key("workingDirectory"), workDir);
	}

	@Override
	public Folder getWorkingDirectory() {
		return getProperty(traits.key("workingDirectory"));
	}

	@Override
	public void setLocalStorage(final String localStorage) throws FrameworkException {
		setProperty(traits.key("localStorage"), localStorage);
	}

	@Override
	public String getLocalStorage() {
		return getProperty(traits.key("localStorage"));
	}
	*/
}
