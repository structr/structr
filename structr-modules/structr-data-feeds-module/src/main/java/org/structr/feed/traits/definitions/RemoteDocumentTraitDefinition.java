/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.feed.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.feed.entity.RemoteDocument;
import org.structr.feed.traits.wrappers.RemoteDocumentTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class RemoteDocumentTraitDefinition extends AbstractNodeTraitDefinition {

	public RemoteDocumentTraitDefinition() {
		super("RemoteDocument");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> urlProperty              = new StringProperty("url");
		final Property<Long> checksumProperty           = new LongProperty("checksum").readOnly();
		final Property<Integer> cacheForSecondsProperty = new IntProperty("cacheForSeconds");
		final Property<Integer> versionProperty         = new IntProperty("version").readOnly();

		return newSet(
			urlProperty,
			checksumProperty,
			cacheForSecondsProperty,
			versionProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"name", "owner", "url"
			),
			PropertyView.Ui,
			newSet(
				"url", "checksum", "cacheForSeconds", "version"
			)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			RemoteDocument.class, (traits, node) -> new RemoteDocumentTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
