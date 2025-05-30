/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.web.traits.relationships;

import org.structr.core.entity.Relation;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

import java.util.Set;

public class ImageTHUMBNAILImage extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public static final String CHECKSUM_PROPERTY    = "checksum";
	public static final String MAX_WIDTH_PROPERTY   = "maxWidth";
	public static final String MAX_HEIGHT_PROPERTY  = "maxHeight";
	public static final String CROP_TO_FIT_PROPERTY = "cropToFit";

	public ImageTHUMBNAILImage() {
		super(StructrTraits.IMAGE_THUMBNAIL_IMAGE);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.IMAGE;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.IMAGE;
	}

	@Override
	public String getRelationshipType() {
		return "THUMBNAIL";
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
	public Set<PropertyKey> getPropertyKeys() {

		final PropertyKey<Long> checksum     = new LongProperty(CHECKSUM_PROPERTY);
		final PropertyKey<Integer> maxWidth  = new IntProperty(MAX_WIDTH_PROPERTY);
		final PropertyKey<Integer> maxHeight = new IntProperty(MAX_HEIGHT_PROPERTY);
		final PropertyKey<Boolean> cropToFit = new BooleanProperty(CROP_TO_FIT_PROPERTY);

		return Set.of(
			checksum, maxWidth, maxHeight, cropToFit
		);
	}
}
