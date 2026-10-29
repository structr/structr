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
package org.structr.extraction.traits.properties;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DateProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.api.processing.ExtractionStatus;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The extraction state the module adds to File: status, error, date, checksum, options hash, OCR flag,
 * the chunks and the stored figures. The configuration of automatic extraction is not stored on File or Folder but in
 * ContentExtractionConfiguration nodes.
 *
 * Modules cannot add lifecycle methods to existing types, so the write rule is enforced by the
 * properties themselves: only the module's own superuser context can write them.
 */
public final class ContentFileStateProperties {

	public static final String STATUS                        = "contentExtractionStatus";
	public static final String ERROR                         = "contentExtractionError";
	public static final String DATE                          = "contentExtractionDate";
	public static final String CHECKSUM                      = "contentExtractionChecksum";
	public static final String OPTIONS_HASH                  = "contentExtractionOptionsHash";
	public static final String OCR                           = "contentExtractionOcr";
	public static final String EXTRACTED_CONTENT_NODES       = "extractedContentNodes";
	public static final String FIGURES                       = "figures";

	private ContentFileStateProperties() {
	}

	/**
	 * Adds the properties to the File trait. Must run after the relationship types are
	 * registered, because the relationship property resolves its type eagerly.
	 */
	public static void register() {

		final TraitsInstance root = TraitsManager.getRootInstance();
		final Trait file          = Traits.getTrait(StructrTraits.FILE);

		register(file,
			internalEnum(STATUS, statusValues()).indexed().description("Content extraction state: running, done, failed or skipped; null if never extracted."),
			internalString(ERROR).description("Last content extraction error or warning."),
			internalDate(DATE).description("When content extraction last finished."),
			internalLong(CHECKSUM).description("File checksum at the last extraction; unchanged files are skipped."),
			internalString(OPTIONS_HASH).description("Hash of the split and OCR options of the last extraction; changed options cause re-extraction."),
			internalBoolean(OCR).description("Whether OCR contributed text in the last extraction."),
			new EndNodes(root, EXTRACTED_CONTENT_NODES, ContentExtractionTraits.FILE_HAS_EXTRACTED_CONTENT).description("The chunks of this file's content."),
			new EndNodes(root, FIGURES, ContentExtractionTraits.FILE_HAS_FIGURE).description("The figures of this document that were stored as DocumentFigures.")
		);
	}

	// ----- private methods -----
	private static void register(final Trait trait, final PropertyKey... keys) {

		final Set<String> uiView = trait.getViews().computeIfAbsent(PropertyView.Ui, k -> new LinkedHashSet<>());

		for (final PropertyKey key : keys) {

			trait.registerPropertyKey(key);
			uiView.add(key.jsonName());
		}
	}

	private static Set<String> statusValues() {

		return Arrays.stream(ExtractionStatus.values()).map(ExtractionStatus::getValue).collect(Collectors.toSet());
	}

	private static void requireInternal(final SecurityContext securityContext, final PropertyKey key) throws FrameworkException {

		if (securityContext != null && !securityContext.isSuperUserSecurityContext()) {

			throw new FrameworkException(403, key.jsonName() + " is maintained by content extraction and cannot be changed.");
		}
	}

	private static EnumProperty internalEnum(final String name, final Set<String> values) {

		return new EnumProperty(name, values) {

			@Override
			public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final String value) throws FrameworkException {

				requireInternal(securityContext, this);

				return super.setProperty(securityContext, obj, value);
			}
		};
	}

	private static StringProperty internalString(final String name) {

		return new StringProperty(name) {

			@Override
			public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final String value) throws FrameworkException {

				requireInternal(securityContext, this);

				return super.setProperty(securityContext, obj, value);
			}
		};
	}

	private static DateProperty internalDate(final String name) {

		return new DateProperty(name) {

			@Override
			public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final Date value) throws FrameworkException {

				requireInternal(securityContext, this);

				return super.setProperty(securityContext, obj, value);
			}
		};
	}

	private static LongProperty internalLong(final String name) {

		return new LongProperty(name) {

			@Override
			public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final Long value) throws FrameworkException {

				requireInternal(securityContext, this);

				return super.setProperty(securityContext, obj, value);
			}
		};
	}

	private static BooleanProperty internalBoolean(final String name) {

		return new BooleanProperty(name) {

			@Override
			public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final Boolean value) throws FrameworkException {

				requireInternal(securityContext, this);

				return super.setProperty(securityContext, obj, value);
			}
		};
	}
}
