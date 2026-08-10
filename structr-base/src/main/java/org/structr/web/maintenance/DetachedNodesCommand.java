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
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.docs.*;
import org.structr.docs.ontology.ConceptType;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Reports, and on request repairs, DOM nodes that belong to no document.
 *
 * It defaults to a DRY RUN: the damage it looks for is invisible in the UI, so the first thing an
 * administrator needs is to see what is there. Pass repair=true to have it adopt what it can.
 *
 * See DetachedNodes for what the three kinds mean and why the Recycle Bin is never touched.
 */
public class DetachedNodesCommand extends NodeServiceCommand implements MaintenanceCommand {

	private static final Logger logger = LoggerFactory.getLogger(DetachedNodesCommand.class.getName());

	@Override
	public void execute(final Map<String, Object> parameters) throws FrameworkException {

		final boolean repair = "true".equals(String.valueOf(parameters.get("repair")));
		final App app        = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			final List<DetachedNodes.Finding> findings = DetachedNodes.scan(app);
			final List<DetachedNodes.Finding> damaged  = DetachedNodes.damaged(findings);

			logger.info("Found {} DOM node(s) without an ownerDocument: {}", findings.size(), DetachedNodes.countByKind(findings));

			for (final DetachedNodes.Finding finding : findings) {

				logger.info("  {}", finding.describe());
			}

			if (damaged.isEmpty()) {

				logger.info("Nothing to repair: every page-less node is deleted content (Recycle Bin).");

			} else if (repair) {

				logger.info("Repaired {} node(s).", DetachedNodes.repair(app, damaged));

			} else {

				logger.info("{} node(s) can be repaired. This was a DRY RUN - re-run with repair=true to apply it.", damaged.size());
			}

			tx.success();
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

	// ----- interface Documentable -----
	@Override
	public DocumentableType getDocumentableType() {

		return DocumentableType.MaintenanceCommand;
	}

	@Override
	public String getName() {

		return "detachedNodes";
	}

	@Override
	public String getShortDescription() {

		return "Reports (and optionally repairs) DOM nodes that belong to no page.";
	}

	@Override
	public String getLongDescription() {

		return """
        A DOM node that has no ownerDocument still renders, because rendering walks the parent, so the
        application looks healthy. Deployment export walks documents instead - the pages and the
        ShadowDocument - so such a node is silently left out of the export, while every page that
        references it keeps the reference. The next import then produces empty page shells, and a page
        whose root element is such a node fails to import at all.

        The command reports three kinds:

        - `ADOPTABLE`: no document of its own, but an element above it has one. This is damage, and the
          repair gives it that document.
        - `ORPHANED_MASTER`: no document and no parent, but instances of it still exist. It is a shared
          component that lost the ShadowDocument, and the repair puts it back.
        - `RECYCLE_BIN`: no document, no parent, nothing referring to it. That is how deleted content
          looks in Structr, so it is reported and never touched.

        It runs as a DRY RUN unless `repair=true` is passed.
        """;
	}

	@Override
	public List<String> getNotes() {

		return List.of("The same repair runs automatically at startup (see MigrationService), so this command is for inspecting an instance or repairing one without restarting it.");
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
	public List<Link> getLinkedConcepts() {

		final List<Link> links = new LinkedList<>();

		links.add(Link.to("repairs", ConceptReference.of(ConceptType.Topic, "Deployment")));

		return links;
	}
}
