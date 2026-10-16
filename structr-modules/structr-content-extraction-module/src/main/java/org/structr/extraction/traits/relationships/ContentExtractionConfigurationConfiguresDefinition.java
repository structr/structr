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
package org.structr.extraction.traits.relationships;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.extraction.ContentExtractionTraits;

/**
 * ContentExtractionConfiguration -[:CONFIGURES]-> AbstractFile.
 *
 * Only the configuration has a property for it (target), so File and Folder stay unchanged. Structr's
 * relationship helpers on the target side (getIncomingRelationship, the one-to-one cardinality check)
 * need a property on that side, so the relationship is many-to-one: a configuration has exactly one
 * target, and that a target has at most one configuration is validated by the configuration itself.
 *
 * Deleting the file or folder deletes its configuration. Read and delete permissions propagate from the
 * file or folder to the configuration, so whoever may delete a folder may delete it with its
 * configuration.
 */
public class ContentExtractionConfigurationConfiguresDefinition extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ContentExtractionConfigurationConfiguresDefinition() {

		super(ContentExtractionTraits.CONFIGURATION_CONFIGURES);
	}

	@Override
	public String getSourceType() {

		return ContentExtractionTraits.CONTENT_EXTRACTION_CONFIGURATION;
	}

	@Override
	public String getTargetType() {

		return StructrTraits.ABSTRACT_FILE;
	}

	@Override
	public String getRelationshipType() {

		return "CONFIGURES";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {

		return Relation.Multiplicity.Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {

		return Relation.Multiplicity.One;
	}

	@Override
	public int getCascadingDeleteFlag() {

		return Relation.TARGET_TO_SOURCE;
	}

	@Override
	public int getAutocreationFlag() {

		return Relation.NONE;
	}

	@Override
	public boolean isInternal() {

		return false;
	}

	@Override
	public PropagationDirection getPropagationDirection() {

		return PropagationDirection.In;
	}

	@Override
	public PropagationMode getReadPropagation() {

		return PropagationMode.Add;
	}

	@Override
	public PropagationMode getWritePropagation() {

		return PropagationMode.Keep;
	}

	@Override
	public PropagationMode getDeletePropagation() {

		return PropagationMode.Add;
	}

	@Override
	public PropagationMode getAccessControlPropagation() {

		return PropagationMode.Keep;
	}

	@Override
	public String getDeltaProperties() {

		return null;
	}
}
