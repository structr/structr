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
package org.structr.media;

import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.traits.StructrTraits;
import org.structr.media.relationship.VideoFileHAS_CONVERTED_VIDEOVideoFile;
import org.structr.media.relationship.VideoFileHAS_POSTER_IMAGEImage;
import org.structr.media.traits.definitions.VideoFileTraitDefinition;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.definitions.LinkableTraitDefinition;

import java.util.Set;

/**
 *
 */
public class MediaModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		StructrTraits.registerRelationshipType(StructrTraits.VIDEO_FILE_HAS_CONVERTED_VIDEO_VIDEO_FILE, new VideoFileHAS_CONVERTED_VIDEOVideoFile());
		StructrTraits.registerRelationshipType(StructrTraits.VIDEO_FILE_HAS_POSTER_IMAGE_IMAGE,         new VideoFileHAS_POSTER_IMAGEImage());

		StructrTraits.registerNodeType(StructrTraits.VIDEO_FILE, new AbstractFileTraitDefinition(), new FileTraitDefinition(), new LinkableTraitDefinition(), new VideoFileTraitDefinition());
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

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}
}
