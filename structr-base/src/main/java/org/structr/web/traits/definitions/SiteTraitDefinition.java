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
package org.structr.web.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.Site;
import org.structr.web.traits.wrappers.SiteTraitWrapper;

import java.util.Map;
import java.util.Set;

public class SiteTraitDefinition extends AbstractTraitDefinition {

	/*
	TODO: migrate this:
	1. extract interface, rename original class to SiteTraitDefinition
	2. implement SiteTraitWrapper extends AbstractTraitWrapper
	*/

	private static final Property<Iterable<NodeInterface>> pagesProperty = new EndNodes("pages", "SiteCONTAINSPage").partOfBuiltInSchema();
	private static final Property<String> hostnameProperty               = new StringProperty("hostname").indexed().partOfBuiltInSchema();
	private static final Property<Integer> portProperty                  = new IntProperty("port").indexed().partOfBuiltInSchema();

	/*
	public static final View defaultView = new View(Site.class, PropertyView.Public,
		pagesProperty, hostnameProperty, portProperty
	);

	public static final View uiView = new View(Site.class, PropertyView.Ui,
		pagesProperty, hostnameProperty, portProperty
	);
	*/

	public SiteTraitDefinition() {
		super("Site");
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

		return Map.of(
			Site.class, (traits, node) -> new SiteTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		return Set.of(
			pagesProperty,
			hostnameProperty,
			portProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
