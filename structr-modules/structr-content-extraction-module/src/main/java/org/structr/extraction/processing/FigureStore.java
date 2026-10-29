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
package org.structr.extraction.processing;

import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.Permissions;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.extraction.api.document.ExtractedFigureData;
import org.structr.extraction.traits.definitions.DocumentFigureTraitDefinition;
import org.structr.extraction.traits.properties.ContentFileStateProperties;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores the figures of a document as DocumentFigures (or a type that inherits it) in a folder next to
 * the document, "&lt;document name&gt; figures". The folder and the figures get the owner, the visibility
 * flags and the grants of the document; afterwards they are files of their own and are kept when the
 * document is deleted.
 *
 * When the document is extracted again, a figure with the same image and label keeps its node: what an
 * application added to it (a selection, its own text) stays, only where the figure is and its caption
 * are updated. Figures the document no longer has are deleted.
 */
public final class FigureStore {

	private FigureStore() {
	}

	/**
	 * Must run in a transaction.
	 *
	 * @param figureType the type to create figures as
	 * @return the ids of the document's figures, in reading order
	 */
	public static List<String> replace(final File document, final List<ExtractedFigureData> figures, final String figureType) throws FrameworkException {

		final SecurityContext superUser                = SecurityContext.getSuperUserInstance();
		final App app                                  = StructrApp.getInstance(superUser);
		final Map<String, List<NodeInterface>> byKey   = new HashMap<>();
		final List<String> uuids                       = new ArrayList<>();
		final Iterable<NodeInterface> existing         = document.getProperty(Traits.of(StructrTraits.FILE).key(ContentFileStateProperties.FIGURES));

		for (final NodeInterface figure : existing) {

			final Traits traits = figure.getTraits();

			byKey.computeIfAbsent(key(figure.getProperty(traits.key(DocumentFigureTraitDefinition.IMAGE_HASH_PROPERTY)), figure.getProperty(traits.key(DocumentFigureTraitDefinition.LABEL_PROPERTY))), k -> new ArrayList<>()).add(figure);
		}

		Folder folder = null;

		for (final ExtractedFigureData data : figures) {

			final List<NodeInterface> same = byKey.get(key(data.checksum(), data.label()));

			if (same != null && !same.isEmpty()) {

				// the text stays: it may be what an application wrote instead of the extracted one
				final NodeInterface kept = same.removeFirst();

				kept.setProperties(superUser, metadata(kept.getTraits(), data));
				copyPermissions(document, kept);
				uuids.add(kept.getUuid());

			} else {

				if (folder == null) {

					folder = folder(document);
				}

				uuids.add(create(app, document, folder, data, figureType).getUuid());
			}
		}

		for (final List<NodeInterface> unused : byKey.values()) {

			for (final NodeInterface figure : unused) {

				app.delete(figure);
			}
		}

		return uuids;
	}

	// ----- private methods -----
	private static NodeInterface create(final App app, final File document, final Folder folder, final ExtractedFigureData data, final String figureType) throws FrameworkException {

		final SecurityContext superUser = SecurityContext.getSuperUserInstance();
		final Traits traits             = Traits.of(figureType);
		final PropertyMap properties    = metadata(traits, data);
		final String label              = data.label() != null ? data.label() : "Figure " + (data.position() + 1);

		properties.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), document.getName() + " - " + label + ".png");
		properties.put(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder);
		properties.put(traits.key(DocumentFigureTraitDefinition.DOCUMENT_PROPERTY), document);
		properties.put(traits.key(DocumentFigureTraitDefinition.TEXT_PROPERTY), data.text().isBlank() ? null : data.text());

		final NodeInterface figure = app.create(figureType, properties);

		try {

			FileHelper.setFileData(figure.as(File.class), data.image(), data.mediaType(), true);
			ImageHelper.updateMetadata(figure.as(File.class));

		} catch (final IOException ex) {

			throw new FrameworkException(500, "Unable to store the image of " + label + " of " + document.getName() + ": " + ex.getMessage());
		}

		copyPermissions(document, figure);

		return figure;
	}

	/**
	 * What extraction found out about where the figure is; the name, the folder and the text are left to
	 * the creation, so an application can change them.
	 */
	private static PropertyMap metadata(final Traits traits, final ExtractedFigureData data) {

		final PropertyMap properties = new PropertyMap();

		properties.put(traits.key(DocumentFigureTraitDefinition.PAGE_PROPERTY), data.page());
		properties.put(traits.key(DocumentFigureTraitDefinition.POSITION_PROPERTY), data.position());
		properties.put(traits.key(DocumentFigureTraitDefinition.LABEL_PROPERTY), data.label());
		properties.put(traits.key(DocumentFigureTraitDefinition.CAPTION_PROPERTY), data.caption());
		properties.put(traits.key(DocumentFigureTraitDefinition.SOURCE_PROPERTY), data.source().getValue());
		properties.put(traits.key(DocumentFigureTraitDefinition.BBOX_PROPERTY), data.bbox() != null ? Arrays.stream(data.bbox()).boxed().toArray(Double[]::new) : null);
		properties.put(traits.key(DocumentFigureTraitDefinition.IMAGE_HASH_PROPERTY), data.checksum());
		properties.put(traits.key(DocumentFigureTraitDefinition.MENTIONS_PROPERTY), data.mentions().toArray(String[]::new));

		return properties;
	}

	/**
	 * "&lt;document name&gt; figures" in the document's folder, created with the document's permissions if it
	 * does not exist yet.
	 */
	private static Folder folder(final File document) throws FrameworkException {

		final Folder parent = document.getParent();
		final String path   = (parent != null ? parent.getPath() : "") + "/" + document.getName() + " figures";
		final Folder folder = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), path).as(Folder.class);

		copyPermissions(document, folder);

		return folder;
	}

	/**
	 * The owner, the visibility flags and the grants of the document, added to a node.
	 */
	private static void copyPermissions(final File document, final NodeInterface target) throws FrameworkException {

		final SecurityContext superUser                = SecurityContext.getSuperUserInstance();
		final Traits traits                            = target.getTraits();
		final PropertyKey<NodeInterface> ownerKey      = traits.key(NodeInterfaceTraitDefinition.OWNER_PROPERTY);
		final PropertyKey<Boolean> publicKey           = traits.key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
		final PropertyKey<Boolean> authenticatedKey    = traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY);
		final PropertyMap properties                   = new PropertyMap();
		final NodeInterface owner                      = document.getProperty(document.getTraits().key(NodeInterfaceTraitDefinition.OWNER_PROPERTY));

		if (owner != null && target.getProperty(ownerKey) == null) {

			properties.put(ownerKey, owner);
		}

		properties.put(publicKey, document.isVisibleToPublicUsers());
		properties.put(authenticatedKey, document.isVisibleToAuthenticatedUsers());

		target.setProperties(superUser, properties);

		for (final Security security : document.as(AccessControllable.class).getSecurityRelationships()) {

			final Principal principal = security.getSourceNode();
			if (principal != null) {

				final Set<Permission> permissions = new LinkedHashSet<>();

				for (final String permission : security.getPermissions()) {

					final Permission value = Permissions.valueOf(permission);
					if (value != null) {

						permissions.add(value);
					}
				}

				if (!permissions.isEmpty()) {

					target.as(AccessControllable.class).grant(permissions, principal, superUser);
				}
			}
		}
	}

	private static String key(final String imageHash, final String label) {

		return imageHash + "|" + label;
	}
}
