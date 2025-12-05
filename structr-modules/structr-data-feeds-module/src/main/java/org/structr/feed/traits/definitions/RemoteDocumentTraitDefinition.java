/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.feed.entity.RemoteDocument;
import org.structr.feed.traits.wrappers.RemoteDocumentTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class RemoteDocumentTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String URL_PROPERTY               = "url";
	public static final String CONTENT_TYPE_PROPERTY      = "contentType";
	public static final String CHECKSUM_PROPERTY          = "checksum";
	public static final String CACHE_FOR_SECONDS_PROPERTY = "cacheForSeconds";
	public static final String VERSION_PROPERTY           = "version";

	public RemoteDocumentTraitDefinition() {
		super(StructrTraits.REMOTE_DOCUMENT);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<String> urlProperty              = new StringProperty(URL_PROPERTY);
		final Property<String> contentTypeProperty      = new StringProperty(CONTENT_TYPE_PROPERTY);
		final Property<Long> checksumProperty           = new LongProperty(CHECKSUM_PROPERTY).readOnly();
		final Property<Integer> cacheForSecondsProperty = new IntProperty(CACHE_FOR_SECONDS_PROPERTY);
		final Property<Integer> versionProperty         = new IntProperty(VERSION_PROPERTY).readOnly();

		return newSet(
			urlProperty,
			contentTypeProperty,
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
					NodeInterfaceTraitDefinition.OWNER_PROPERTY, URL_PROPERTY, CONTENT_TYPE_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					URL_PROPERTY, CHECKSUM_PROPERTY, CACHE_FOR_SECONDS_PROPERTY, VERSION_PROPERTY, CONTENT_TYPE_PROPERTY
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
