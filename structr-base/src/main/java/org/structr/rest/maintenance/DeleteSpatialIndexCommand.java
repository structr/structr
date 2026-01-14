/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.rest.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.api.graph.Node;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.LocationTraitDefinition;
import org.structr.docs.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 *
 *
 */
public class DeleteSpatialIndexCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(DeleteSpatialIndexCommand.class.getName());

	@Override
	public void execute(Map<String, Object> attributes) throws FrameworkException {


		final DatabaseService graphDb = StructrApp.getInstance().getService(NodeService.class).getDatabaseService();
		final List<Node> toDelete     = new LinkedList<>();

		for (final Node node: graphDb.getAllNodes()) {

			try {
				if (node.hasProperty("bbox") && node.hasProperty("gtype") && node.hasProperty(GraphObjectTraitDefinition.ID_PROPERTY) && node.hasProperty(LocationTraitDefinition.LATITUDE_PROPERTY) && node.hasProperty(LocationTraitDefinition.LONGITUDE_PROPERTY)) {

					toDelete.add(node);
				}

			} catch (Throwable t) {}

		}

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			for (Node node : toDelete) {

				logger.info("Deleting node {}", node);

				try {

					node.delete(true);

				} catch (Throwable t) {

					logger.warn("", t);
				}

			}

			tx.success();
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return false;
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.MaintenanceCommand;
	}

	@Override
	public String getName() {
		return "deleteSpatialIndex";
	}

	@Override
	public String getShortDescription() {
		return "Removes a (broken) spatial index from the database.";
	}

	@Override
	public String getLongDescription() {
		return "This command deletes all Structr nodes with the properties `bbox` and `gtype`.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of();
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This is a legacy command which you will probably never need."
		);
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
