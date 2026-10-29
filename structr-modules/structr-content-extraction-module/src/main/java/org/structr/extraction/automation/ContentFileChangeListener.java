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
package org.structr.extraction.automation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.StructrTransactionListener;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.scheduler.JobQueueManager;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.extraction.ContentExtractionSettings;
import org.structr.extraction.ContentExtractionTraits;
import org.structr.extraction.options.ContentOptionsResolver;
import org.structr.extraction.processing.ContentExtractionJob;
import org.structr.web.entity.File;
import org.structr.web.traits.definitions.FileTraitDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Starts automatic extraction for files that got new content or were moved into a folder whose content
 * extraction configuration enables it.
 *
 * Content changes are recognized by the file version, which Structr increases whenever new content has
 * been written completely: an upload over REST, the UI, WebDAV or SFTP, or a file created by a script.
 * That covers every way content arrives without a hook in each of them. (The checksum would be the
 * more natural signal, but it is written as a system property, which the modification events do not
 * list.) Re-uploading identical content starts a job that skips the file, because its checksum is
 * unchanged. Moves are recognized by a new folder relationship to a node that existed before the
 * transaction.
 */
public class ContentFileChangeListener implements StructrTransactionListener {

	private static final Logger logger = LoggerFactory.getLogger(ContentFileChangeListener.class);

	private static final String CONTAINS = "CONTAINS";

	/**
	 * Collects the files to extract while the transaction is still open, and starts the job once it has
	 * committed.
	 *
	 * This works in beforeCommit rather than afterCommit on purpose: Structr only calls afterCommit for
	 * transactions with notifications enabled, which by default are those of the admin UI's websocket
	 * commands. A REST upload, a script or a WebDAV client would never trigger extraction there.
	 * beforeCommit is called for every top-level transaction, and a post-commit procedure runs only if
	 * the commit succeeds, so a failed transaction starts nothing.
	 */
	@Override
	public void beforeCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) {

		if (!ContentExtractionSettings.AutoExtractionEnabled.getValue()) {

			return;
		}

		try {

			final Set<String> candidates = candidates(modificationEvents);
			if (!candidates.isEmpty()) {

				final List<String> uuids = List.copyOf(candidates);
				final String queue       = ContentExtractionSettings.ExtractionQueue.getValue();

				// whether a file is marked is decided after the commit: before it, a moved file still
				// reports its old parent folder
				TransactionCommand.queuePostCommitProcedure(() -> startJob(uuids, queue));
			}

		} catch (final Throwable t) {

			// automatic extraction must never interfere with the transaction that triggered it
			logger.warn("Unable to start automatic content extraction: {}", t.getMessage(), t);
		}
	}

	@Override
	public void afterCommit(final SecurityContext securityContext, final Collection<ModificationEvent> modificationEvents) {
	}

	@Override
	public void simpleBroadcast(final String messageName, final Map<String, Object> data, final Predicate<String> sessionIdPredicate) {
	}

	// ----- private methods -----
	/**
	 * Runs after the commit, in the committing thread. Files are checked here, which is cheap, so that
	 * uploads outside marked folders do not start empty jobs; moved folders are left to the job, which
	 * walks their subtree in its own thread.
	 */
	private static void startJob(final List<String> uuids, final String queue) {

		try {

			final App app              = StructrApp.getInstance(SecurityContext.getSuperUserInstance());
			final List<String> targets = new ArrayList<>();

			try (final Tx tx = app.tx()) {

				for (final String uuid : uuids) {

					final NodeInterface node = app.getNodeById(uuid);
					if (node == null) {

						continue;
					}

					if (node.is(StructrTraits.FOLDER) || (node.is(StructrTraits.FILE) && ContentOptionsResolver.shouldExtract(node.as(File.class)).extract())) {

						targets.add(uuid);
					}
				}

				tx.success();
			}

			if (!targets.isEmpty()) {

				JobQueueManager.getInstance().addJob(ContentExtractionJob.automatic(targets, queue));
			}

		} catch (final Throwable t) {

			logger.warn("Unable to start automatic content extraction: {}", t.getMessage(), t);
		}
	}

	/**
	 * Files with new content, files moved into another folder, and folders moved into another folder.
	 */
	private static Set<String> candidates(final Collection<ModificationEvent> events) {

		final PropertyKey<Integer> versionKey = Traits.of(StructrTraits.FILE).key(FileTraitDefinition.VERSION_PROPERTY);
		final Set<String> createdNodes        = new HashSet<>();
		final Set<String> candidates          = new LinkedHashSet<>();

		for (final ModificationEvent event : events) {

			if (event.isNode() && event.isCreated()) {

				createdNodes.add(event.getUuid());
			}
		}

		for (final ModificationEvent event : events) {

			if (event.isDeleted()) {

				continue;
			}

			final GraphObject object = event.getGraphObject();

			if (event.isNode()) {

				// the figures of a document are content extraction's own output, never its input
				if (object instanceof NodeInterface node && node.is(StructrTraits.FILE) && !node.is(ContentExtractionTraits.DOCUMENT_FIGURE) && (hasNewVersion(event, node, versionKey) || isCreatedWithContent(event, node))) {

					candidates.add(event.getUuid());
				}

			} else if (event.isCreated() && event.getRelationshipType() != null && CONTAINS.equals(event.getRelationshipType().name()) && object instanceof RelationshipInterface rel) {

				final NodeInterface source = rel.getSourceNodeAsSuperUser();
				final NodeInterface target = rel.getTargetNodeAsSuperUser();

				// a node created in the same transaction is new, not moved; new files are recognized by their content
				if (source != null && target != null && source.is(StructrTraits.FOLDER) && !createdNodes.contains(target.getUuid())) {

					candidates.add(target.getUuid());
				}
			}
		}

		return candidates;
	}

	/**
	 * Whether the file has new, complete content according to its version.
	 *
	 * File's AfterCreation callback sets version 0, in a transaction of its own after the file was
	 * created. That is new content only if the file is not empty: a file created and filled in one
	 * transaction (e.g. by a script) shows up here with its content. An upload in chunks (Files UI)
	 * creates an empty file first, so version 0 is ignored, and the last chunk increases the version to
	 * 1 or more once all content is written.
	 */
	private static boolean hasNewVersion(final ModificationEvent event, final NodeInterface node, final PropertyKey<Integer> versionKey) {

		if (!event.getModifiedProperties().containsKey(versionKey) && !event.getNewProperties().containsKey(versionKey)) {

			return false;
		}

		final Integer version = node.getProperty(versionKey);
		final Long size       = node.as(File.class).getSize();

		return (version != null && version > 0) || (size != null && size > 0);
	}

	/**
	 * A file created and filled in one transaction (e.g. by a script) does not list its version as
	 * modified. It has content if its checksum was computed and it is not empty: AfterCreation computes
	 * the checksum of an empty file, too.
	 */
	private static boolean isCreatedWithContent(final ModificationEvent event, final NodeInterface node) {

		final File file = node.as(File.class);
		final Long size = file.getSize();

		return event.isCreated() && file.getChecksum() != null && size != null && size > 0;
	}
}
