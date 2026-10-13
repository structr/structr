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
package org.structr.core.graph;

import org.structr.common.error.FrameworkException;
import org.structr.api.DatabaseService;
import org.structr.core.Services;
import org.structr.docs.*;
import org.structr.docs.ontology.ConceptType;

import java.util.List;
import java.util.Map;

/**
 * Returns the prefetch statistics of the database driver, optionally resetting them afterwards.
 */
public class PrefetchStatisticsCommand extends NodeServiceCommand implements MaintenanceCommand {

	private Map<String, Object> result = null;

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final Object reset = attributes.get("reset");
		final DatabaseService db = Services.getInstance().getDatabaseService();

		result = db.getPrefetchStatistics();

		if (Boolean.TRUE.equals(reset) || "true".equals(reset)) {

			db.resetPrefetchStatistics();
		}
	}

	@Override
	public Object getCommandResult() {

		return result;
	}

	@Override
	public boolean requiresEnclosingTransaction() {

		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {

		return false;
	}

	@Override
	public DocumentableType getDocumentableType() {

		return DocumentableType.MaintenanceCommand;
	}

	@Override
	public String getName() {

		return "prefetchStatistics";
	}

	@Override
	public String getShortDescription() {

		return "Returns what the automatic prefetching costs and saves, per kind of request.";
	}

	@Override
	public String getLongDescription() {

		return """
		The statistics are grouped by prefetch hint, which names a kind of request, such as `HTTP GET /projects/<uuid>` or `REST GET /structr/rest/Project`. For each hint, `withPrefetching` and `withoutPrefetching` show the number of transactions and their average duration, number of queries and number of relationship lookup queries, depending on whether `database.prefetching.enabled` was on when the transaction started. Turning the setting off for a while shows what prefetching saves for each kind of request.

		`patterns` lists the patterns learned for the hint, with their status, how often they ran, how many entities they loaded, how many lookups they answered per execution and how often their evaluation failed. A pattern pays off when it loads few entities per saved lookup, see `database.prefetching.costratio`, and when it takes less time (`averageMillis`) than the lookups it saves would have taken (`averageSavedMillis`, estimated from `averageLookupMillis`, the average time of a relationship lookup query).
		""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.optional("reset", "`true` resets the statistics after returning them")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of();
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The statistics are kept in memory and start over when Structr restarts.",
			"Only the bolt driver supports prefetching, other drivers return no statistics."
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

	@Override
	public final List<ConceptReference> getParentConcepts() {

		return List.of(ConceptReference.of(ConceptType.Topic, "Maintenance Commands"));
	}
}
