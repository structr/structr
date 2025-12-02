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
import org.structr.api.DatabaseService;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.docs.*;

import java.util.List;
import java.util.Map;

/**
 * Clears database.
 *
 * This command takes no parameters.
 */
public class ClearDatabase extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(ClearDatabase.class.getName());

	public void execute() throws FrameworkException {
		execute(Map.of());
	}

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		FlushCachesCommand.flushAll();

		final DatabaseService graphDb = (DatabaseService) arguments.get("graphDb");
		if (graphDb != null) {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				graphDb.cleanDatabase();

				tx.success();
			}

		} else {

			logger.error("Unable to clear database: no database service found.");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return true;
	}

	@Override
	public boolean requiresFlushingOfCaches() {
		return true;
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.MaintenanceCommand;
	}

	@Override
	public String getName() {
		return "clearDatabase";
	}

	@Override
	public String getShortDescription() {
		return "Clears the database, i.e. removes all nodes and relationships from the database.";
	}

	@Override
	public String getLongDescription() {
		return "";
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
			"Warning: this action cannot be reversed, it will delete your application and everything else in the database, even non-Structr nodes and relationships."
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
