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
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.Site;
import org.structr.web.traits.wrappers.SiteTraitWrapper;

import java.util.Map;
import java.util.Set;

public class SiteTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PAGES_PROPERTY    = "pages";
	public static final String HOSTNAME_PROPERTY = "hostname";
	public static final String PORT_PROPERTY     = "port";

	public SiteTraitDefinition() {
		super(StructrTraits.SITE);
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

		final Property<Iterable<NodeInterface>> pagesProperty = new EndNodes(PAGES_PROPERTY, StructrTraits.SITE_CONTAINS_PAGE);
		final Property<String> hostnameProperty               = new StringProperty(HOSTNAME_PROPERTY).indexed();
		final Property<Integer> portProperty                  = new IntProperty(PORT_PROPERTY).indexed();

		return Set.of(
			pagesProperty,
			hostnameProperty,
			portProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					PAGES_PROPERTY, HOSTNAME_PROPERTY, PORT_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					PAGES_PROPERTY, HOSTNAME_PROPERTY, PORT_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
