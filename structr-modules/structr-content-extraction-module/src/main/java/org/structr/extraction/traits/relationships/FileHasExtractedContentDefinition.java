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
import org.structr.extraction.entity.ExtractedContentNode;

/**
 * File -[:HAS_EXTRACTED_CONTENT]-> ExtractedContentNode.
 *
 * Read permission propagates from the file to its chunks, so a chunk is never visible to someone who
 * cannot read the file, and the chunks need no grants of their own. Deleting the file deletes them.
 */
public class FileHasExtractedContentDefinition extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public FileHasExtractedContentDefinition() {

		super(ContentExtractionTraits.FILE_HAS_EXTRACTED_CONTENT);
	}

	@Override
	public String getSourceType() {

		return StructrTraits.FILE;
	}

	@Override
	public String getTargetType() {

		return ContentExtractionTraits.EXTRACTED_CONTENT_NODE;
	}

	@Override
	public String getRelationshipType() {

		return "HAS_EXTRACTED_CONTENT";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {

		return Relation.Multiplicity.One;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {

		return Relation.Multiplicity.Many;
	}

	@Override
	public int getCascadingDeleteFlag() {

		return Relation.SOURCE_TO_TARGET;
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

		return PropagationDirection.Out;
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

		return PropagationMode.Keep;
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
