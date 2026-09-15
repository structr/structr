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
package org.structr.web.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.MigrationService;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.docs.*;

import java.util.List;
import java.util.Map;

/**
 * Runs the startup migrations on demand.
 *
 * The mode is a parameter here rather than the configured one, so an instance set to `apply` can still
 * be asked what a migration would do, and an instance left on `dry-run` can be migrated without editing
 * its configuration and restarting.
 *
 * Unlike at startup, a dry run does NOT stop anything: the instance is already running, and the
 * rollback leaves the database as it was.
 */
public class MigrationCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(MigrationCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> parameters) throws FrameworkException {

		final String mode = parameters.containsKey("mode") ? String.valueOf(parameters.get("mode")) : MigrationService.DRY_RUN;
		if (!MigrationService.DRY_RUN.equals(mode) && !"apply".equals(mode)) {

			throw new FrameworkException(422, "Unknown mode '" + mode + "', expected '" + MigrationService.DRY_RUN + "' or 'apply'.");
		}

		final boolean dryRun = MigrationService.DRY_RUN.equals(mode);

		logger.info("Running the migrations in '{}' mode.", mode);

		MigrationService.execute(dryRun);

		if (dryRun) {

			logger.info("Dry run finished, nothing was changed. Run this command with mode=apply to migrate.");

		} else {

			logger.info("Migration finished.");
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {

		// every step opens its own transaction, and a dry run needs to roll its own back

		return false;
	}

	@Override
	public boolean requiresFlushingOfCaches() {

		// a migration that applied changes has touched schema and data the caches hold

		return true;
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getDocumentableType() {

		return DocumentableType.MaintenanceCommand;
	}

	@Override
	public String getName() {

		return "migrate";
	}

	@Override
	public String getShortDescription() {

		return "Runs the startup migrations, either reporting what they would change or applying them.";
	}

	@Override
	public String getLongDescription() {

		return """
        Structr migrates data and schema at startup, governed by `application.migration.mode`. This
        command runs the same steps on a running instance, with the mode given as a parameter instead
        of read from the configuration.

        - `mode=dry-run` (default): every step runs and logs what it would change, and the change is
          rolled back. Each step is rolled back on its own, so the command never holds more in one
          transaction than that step would have committed by itself.
        - `mode=apply`: the steps migrate for real.

        Two of the steps only ever read and report: the check for notion properties that need attention,
        and the report on calls to the HTTP functions that still use the pre-7.0 signature. Those run in
        both modes.

        The results are written to the server log, not returned to the caller.
        """;
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"Unlike the dry run at startup, this one does not stop the instance: it is already running, and the rollback leaves the database as it was.",
			"On a cluster the migrations run on the coordinator only, so this command does nothing on the other members."
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
