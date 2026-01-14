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
package org.structr.media;

import org.structr.api.service.LicenseManager;
import org.structr.core.property.EndNode;
import org.structr.core.property.StartNode;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.media.relationship.VideoFileHAS_CONVERTED_VIDEOVideoFile;
import org.structr.media.relationship.VideoFileHAS_POSTER_IMAGEImage;
import org.structr.media.traits.definitions.VideoFileTraitDefinition;
import org.structr.module.StructrModule;
import org.structr.web.traits.definitions.ImageTraitDefinition;

import java.util.Set;

/**
 *
 */
public class MediaModule implements StructrModule {

	@Override
	public void onLoad() {

		StructrTraits.registerTrait(new VideoFileHAS_CONVERTED_VIDEOVideoFile());
		StructrTraits.registerTrait(new VideoFileHAS_POSTER_IMAGEImage());

		StructrTraits.registerRelationshipType(StructrTraits.VIDEO_FILE_HAS_CONVERTED_VIDEO_VIDEO_FILE, StructrTraits.VIDEO_FILE_HAS_CONVERTED_VIDEO_VIDEO_FILE);
		StructrTraits.registerRelationshipType(StructrTraits.VIDEO_FILE_HAS_POSTER_IMAGE_IMAGE,         StructrTraits.VIDEO_FILE_HAS_POSTER_IMAGE_IMAGE);

		StructrTraits.registerTrait(new VideoFileTraitDefinition());

		StructrTraits.registerNodeType(StructrTraits.VIDEO_FILE, StructrTraits.ABSTRACT_FILE, StructrTraits.FILE, StructrTraits.LINKABLE, StructrTraits.VIDEO_FILE);

		// register VideoFile -> Image relation
		final TraitsInstance rootInstance = TraitsManager.getRootInstance();
		Traits.getTrait(StructrTraits.IMAGE).registerPropertyKey(new StartNode(rootInstance, ImageTraitDefinition.POSTER_IMAGE_OF_VIDEO_PROPERTY, StructrTraits.VIDEO_FILE_HAS_POSTER_IMAGE_IMAGE));
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {
	}

	@Override
	public String getName() {
		return "media";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
