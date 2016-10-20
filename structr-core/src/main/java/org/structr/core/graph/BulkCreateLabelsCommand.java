/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.StructrAndSpatialPredicate;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.SearchCommand;

//~--- classes ----------------------------------------------------------------
/**
 * Create labels for all nodes of the given type.
 */
public class BulkCreateLabelsCommand extends NodeServiceCommand implements MaintenanceCommand, TransactionPostProcess {

	@Override
	public void execute(Map<String, Object> attributes) {

		final String entityType                = (String) attributes.get("type");
		final DatabaseService graphDb          = (DatabaseService) arguments.get("graphDb");
		final SecurityContext superUserContext = SecurityContext.getSuperUserInstance();
		final NodeFactory nodeFactory          = new NodeFactory(superUserContext);

		Iterator<AbstractNode> nodeIterator = null;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			nodeIterator = Iterables.filter(new TypePredicate<>(entityType), Iterables.map(nodeFactory, Iterables.filter(new StructrAndSpatialPredicate(true, false, false), graphDb.getAllNodes()))).iterator();
			tx.success();

		} catch (FrameworkException fex) {
			warn("Exception while creating all nodes iterator.", fex);
		}

		if (entityType == null) {

			info("Node type not set or no entity class found. Starting creation of labels for all nodes.");

		} else {

			info("Starting creation of labels for all nodes of type {}", entityType);
		}

		final long count = bulkGraphOperation(securityContext, nodeIterator, 10000, "CreateLabels", new BulkGraphOperation<AbstractNode>() {

			@Override
			public void handleGraphObject(SecurityContext securityContext, AbstractNode node) {

				final Set<Label> intersection = new LinkedHashSet<>();
				final Set<Label> toRemove     = new LinkedHashSet<>();
				final Set<Label> toAdd        = new LinkedHashSet<>();
				final Node dbNode             = node.getNode();

				// collect labels that are already present on a node
				for (final Label label : dbNode.getLabels()) {
					toRemove.add(label);
				}

				// collect new labels
				for (final Class supertype : SearchCommand.typeAndAllSupertypes(node.getClass())) {

					final String supertypeName = supertype.getName();

					if (supertypeName.startsWith("org.structr.") || supertypeName.startsWith("com.structr.")) {
						toAdd.add(graphDb.forName(Label.class, supertype.getSimpleName()));
					}
				}

				// calculate intersection
				intersection.addAll(toAdd);
				intersection.retainAll(toRemove);

				// calculate differences
				toAdd.removeAll(intersection);
				toRemove.removeAll(intersection);

				// remove difference
				for (final Label remove : toRemove) {
					dbNode.removeLabel(remove);
				}

				// add difference
				for (final Label add : toAdd) {
					dbNode.addLabel(add);
				}
			}

			@Override
			public void handleThrowable(SecurityContext securityContext, Throwable t, AbstractNode node) {
				warn("Unable to create labels for node {}: {}", node, t.getMessage());
			}

			@Override
			public void handleTransactionFailure(SecurityContext securityContext, Throwable t) {
				warn("Unable to create labels for node: {}", t.getMessage());
			}
		});

		info("Done with creating labels on {} nodes", count);
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	// ----- interface TransactionPostProcess -----
	@Override
	public boolean execute(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		execute(Collections.EMPTY_MAP);

		return true;
	}
}
