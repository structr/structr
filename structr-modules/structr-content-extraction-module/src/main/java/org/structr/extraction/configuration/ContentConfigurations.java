/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.extraction.configuration;

import org.structr.api.graph.Direction;
import org.structr.api.graph.Relationship;
import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.traits.Traits;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.entity.ContentExtractionConfiguration;
import org.structr.web.entity.AbstractFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the content extraction configuration of a File or Folder.
 *
 * File and Folder have no property for the configuration; it is found through the raw incoming CONFIGURES
 * relationships (Structr's relationship helpers need a property on the File side), as superuser, since configurations are an admin concern and the decision must not
 * depend on who uploaded a file. Nothing is cached, like the storage configuration lookup.
 */
public final class ContentConfigurations {

	/**
	 * A configuration and the File or Folder it is attached to.
	 */
	public record Found(ContentExtractionConfiguration configuration, AbstractFile target) {

		public String targetPath() {

			return target.getPath();
		}
	}

	private ContentConfigurations() {
	}

	/**
	 * The configuration attached to the node itself, or null.
	 */
	public static ContentExtractionConfiguration own(final AbstractFile node) {

		final List<ContentExtractionConfiguration> attached = attached(node);

		return attached.isEmpty() ? null : attached.getFirst();
	}

	/**
	 * All configurations attached to the node; more than one only while a transaction that adds one is
	 * validated.
	 */
	public static List<ContentExtractionConfiguration> attached(final NodeInterface node) {

		final List<ContentExtractionConfiguration> configurations = new ArrayList<>();

		if (node.getNode() == null) {

			return configurations;
		}

		final RelationshipFactory factory = new RelationshipFactory(SecurityContext.getSuperUserInstance());
		final Iterable<? extends Relationship<?>> raw = node.getNode().getRelationships(Direction.INCOMING, Traits.of(ContentExtractionTraits.CONFIGURATION_CONFIGURES).getRelation());

		for (final Relationship<?> relationship : raw) {

			final RelationshipInterface rel = factory.adapt(relationship);
			final NodeInterface source      = rel != null ? rel.getSourceNode() : null;

			if (source != null && source.is(ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION)) {

				configurations.add(source.as(ContentExtractionConfiguration.class));
			}
		}

		return configurations;
	}

	/**
	 * The nearest configuration: the node's own, otherwise the nearest one of its ancestor folders, or
	 * null.
	 */
	public static Found nearest(final AbstractFile node) {

		for (AbstractFile current = node; current != null; current = current.getParent()) {

			final ContentExtractionConfiguration configuration = own(current);
			if (configuration != null) {

				return new Found(configuration, current);
			}
		}

		return null;
	}

	/**
	 * The configuration the node would use without its own one: the nearest one of its ancestor
	 * folders, or null.
	 */
	public static Found inherited(final AbstractFile node) {

		return node.getParent() != null ? nearest(node.getParent()) : null;
	}
}
