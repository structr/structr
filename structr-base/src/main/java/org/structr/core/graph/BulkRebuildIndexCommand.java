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
package org.structr.core.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractRelationship;
import org.structr.docs.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Rebuild index for nodes or relationships of given type.
 *
 * Use 'type' argument for node type, and 'relType' for relationship type.
 *
 */
public class BulkRebuildIndexCommand extends NodeServiceCommand implements MaintenanceCommand, TransactionPostProcess {

	private static final Logger logger = LoggerFactory.getLogger(BulkRebuildIndexCommand.class.getName());

	@Override
	public void execute(Map<String, Object> attributes) {

		final String mode       = (String) attributes.get("mode");
		final String entityType = (String) attributes.get("type");
		final String relType    = (String) attributes.get("relType");

		if (mode == null || "nodesOnly".equals(mode)) {
			rebuildNodeIndex(entityType);
		}

		if (mode == null || "relsOnly".equals(mode)) {
			rebuildRelationshipIndex(relType);
		}

		if ("fulltext".equals(mode)) {
			rebuildFulltextIndex();
		}
	}

	// ----- interface TransactionPostProcess -----
	@Override
	public boolean execute(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		execute(Collections.EMPTY_MAP);

		return true;
	}

	public long executeWithCount(Map<String, Object> attributes) {

		final String mode       = (String) attributes.get("mode");
		final String entityType = (String) attributes.get("type");
		final String relType    = (String) attributes.get("relType");

		if (mode == null || "nodesOnly".equals(mode)) {
			return rebuildNodeIndex(entityType);
		}

		if (mode == null || "relsOnly".equals(mode)) {
			return rebuildRelationshipIndex(relType);
		}

		if ("fulltext".equals(mode)) {
			return rebuildFulltextIndex();
		}

		return 0;
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
	private long rebuildNodeIndex(final String entityType) {

		if (entityType == null) {

			info("Node type not set or no entity class found. Starting (re-)indexing all nodes");

		} else {

			info("Starting (re-)indexing all nodes of type {}", entityType);
		}

		final long count = bulkGraphOperation(securityContext, getNodeQuery(entityType, true), 1000, "RebuildNodeIndex", new BulkGraphOperation<NodeInterface>() {

			@Override
			public boolean handleGraphObject(final SecurityContext securityContext, final NodeInterface node) {

				node.addToIndex();

				return true;
			}

			@Override
			public void handleThrowable(final SecurityContext securityContext, final Throwable t, final NodeInterface node) {
				logger.warn("Unable to index node {}: {}", node, t.getMessage());
			}

			@Override
			public void handleTransactionFailure(final SecurityContext securityContext, final Throwable t) {
				logger.warn("Unable to index node: {}", t.getMessage());
			}
		});

		info("Done with (re-)indexing {} nodes", count);

		return count;
	}

	private long rebuildRelationshipIndex(final String relType) {

		if (relType == null) {

			info("Relationship type not set, starting (re-)indexing all relationships");

		} else {

			info("Starting (re-)indexing all relationships of type {}", relType);
		}

		final long count = bulkGraphOperation(securityContext, getRelationshipQuery(relType, true), 1000, "RebuildRelIndex", new BulkGraphOperation<AbstractRelationship>() {

			@Override
			public boolean handleGraphObject(final SecurityContext securityContext, final AbstractRelationship rel) {

				rel.addToIndex();

				return true;
			}

			@Override
			public void handleThrowable(final SecurityContext securityContext, final Throwable t, final AbstractRelationship rel) {
				logger.warn("Unable to index relationship {}: {}", rel, t.getMessage());
			}

			@Override
			public void handleTransactionFailure(final SecurityContext securityContext, final Throwable t) {
				logger.warn("Unable to index relationship: {}", t.getMessage());
			}
		});

		info("Done with (re-)indexing {} relationships", count);

		return count;
	}

	private long rebuildFulltextIndex() {

		final long count = bulkGraphOperation(securityContext, StructrApp.getInstance().nodeQuery("Indexable"), 1000, "RebuildFulltextIndex", new BulkGraphOperation<NodeInterface>() {

			@Override
			public boolean handleGraphObject(final SecurityContext securityContext, final NodeInterface indexable) throws FrameworkException {

				StructrApp.getInstance().getFulltextIndexer().addToFulltextIndex(indexable);

				return true;
			}

			@Override
			public void handleThrowable(final SecurityContext securityContext, final Throwable t, final NodeInterface rel) {
				logger.warn("Unable to build fulltext index for {}: {}", rel.getUuid(), t.getMessage());
			}

			@Override
			public void handleTransactionFailure(final SecurityContext securityContext, final Throwable t) {
				logger.warn("Unable to build fulltext index: {}", t.getMessage());
			}
		});

		info("Rebuilding fulltext index done.");

		return count;
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getType() {
		return DocumentableType.MaintenanceCommand;
	}

	@Override
	public String getName() {
		return "rebuildIndex";
	}

	@Override
	public String getShortDescription() {
		return "Rebuilds the internal indexes, either for nodes, or for relationships, or for both.";
	}

	@Override
	public String getLongDescription() {
		return "Rebuilding the index means that all objects are first removed from the index and then added to the index again with all properties that have the `indexed` flag set.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.optional("type", "limit the execution to the given node type"),
			Parameter.optional("relType", "limit the execution to the given relationship"),
			Parameter.optional("mode", "`nodesOnly` or `relsOnly` to rebuild the index only for nodes or relationships")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of();
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of();
	}

	@Override
	public List<Language> getLanguages() {
		return List.of();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of();
	}
}
