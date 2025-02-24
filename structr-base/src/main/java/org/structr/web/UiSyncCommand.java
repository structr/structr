/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.rest.resource.MaintenanceResource;
import org.structr.web.entity.dom.DOMNode;

import java.util.*;

/**
 *
 *
 */
public class UiSyncCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(UiSyncCommand.class.getName());

	static {

		MaintenanceResource.registerMaintenanceCommand("syncUi", UiSyncCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String mode = (String)attributes.get("mode");
		if (mode != null) {

			final String fileName = (String)attributes.get("file");
			if (fileName != null) {

				if ("export".equals(mode)) {

					doExport(fileName);
				}

				if ("import".equals(mode)) {

					doImport(fileName);
				}

			} else {

				throw new FrameworkException(400, "Please specify file name using the file parameter.");
			}

		} else {

			throw new FrameworkException(400, "Please specify mode, must be one of (import|export)");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	// ----- private methods -----
	private void doExport(final String fileName) throws FrameworkException {

		// collect all nodes etc that belong to the frontend (including files)
		// and export them to the given output file
		final Set<RelationshipInterface> rels = new LinkedHashSet<>();
		final Set<NodeInterface> nodes        = new LinkedHashSet<>();
		final Set<String> filePaths           = new LinkedHashSet<>();
		final App app                         = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			// collect folders that are marked for export
			for (final NodeInterface folder : app.nodeQuery(StructrTraits.FOLDER).and(Traits.of(StructrTraits.FOLDER).key("includeInFrontendExport"), true).getResultStream()) {

				collectDataRecursively(app, folder, nodes, rels, filePaths);
			}

			// collect pages (including files, shared components etc.)
			for (final NodeInterface page : app.nodeQuery(StructrTraits.PAGE).getResultStream()) {

				collectDataRecursively(app, page, nodes, rels, filePaths);
			}

			SyncCommand.exportToFile(fileName, nodes, rels, filePaths, true);

			tx.success();
		}
	}

	private void doImport(final String fileName) throws FrameworkException {

		final App app                 = StructrApp.getInstance();
		final DatabaseService graphDb = app.getDatabaseService();

		SyncCommand.importFromFile(graphDb, securityContext, fileName, true);

		// import done, now the ShadowDocument needs some special care. :(
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> shadowDocuments = app.nodeQuery(StructrTraits.SHADOW_DOCUMENT).includeHidden().getAsList();
			if (shadowDocuments.size() > 1) {

				final PropertyKey<List<DOMNode>> elementsKey = Traits.of(StructrTraits.PAGE).key("elements");
				final List<DOMNode> collectiveChildren       = new LinkedList<>();

				// sort by node id (higher node ID is newer entity)
				Collections.sort(shadowDocuments, new Comparator<>() {

					@Override
					public int compare(final NodeInterface t1, final NodeInterface t2) {
						return t2.getPropertyContainer().getId().compareTo(t1.getPropertyContainer().getId());
					}
				});

				final NodeInterface previousShadowDoc = shadowDocuments.get(0);
				final NodeInterface newShadowDoc      = shadowDocuments.get(1);

				// collect children of both shadow documents
				collectiveChildren.addAll(previousShadowDoc.getProperty(elementsKey));
				collectiveChildren.addAll(newShadowDoc.getProperty(elementsKey));

				// delete old shadow document
				app.delete(previousShadowDoc);

				// add children to new shadow document
				newShadowDoc.setProperties(securityContext, new PropertyMap(elementsKey, collectiveChildren));
			}

			tx.success();
		}
	}

	private void collectDataRecursively(final App app, final GraphObject root, final Set<NodeInterface> nodes, final Set<RelationshipInterface> rels, final Set<String> files) throws FrameworkException {

		if (root.isNode()) {

			final NodeInterface node = root.getSyncNode();
			if (node.is(StructrTraits.FILE)) {

				final String fileUuid = node.getUuid();
				files.add(fileUuid);
			}

			// add node to set, recurse if not already present
			if (nodes.add(node)) {

				final List<GraphObject> syncData = node.getSyncData();
				if (syncData != null) {

					for (final GraphObject obj : syncData) {

						// syncData can contain null objects!
						if (obj != null) {

							collectDataRecursively(app, obj, nodes, rels, files);
						}
					}

				} else {

					logger.warn("Node {} returned null syncData!", node);
				}
			}


		} else if (root.isRelationship()) {

			final RelationshipInterface rel = root.getSyncRelationship();

			// add node to set, recurse if not already present
			if (rels.add(rel)) {

				final List<GraphObject> syncData = rel.getSyncData();
				if (syncData != null) {

					for (final GraphObject obj : syncData) {

						// syncData can contain null objects!
						if (obj != null) {

							collectDataRecursively(app, obj, nodes, rels, files);
						}
					}

				} else {

					logger.warn("Relationship {} returned null syncData!", rel);
				}
			}
		}
	}
}
