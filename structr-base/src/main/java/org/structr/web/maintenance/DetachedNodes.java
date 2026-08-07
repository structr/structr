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
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.command.CreateComponentCommand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds and repairs DOM nodes that belong to no document, the damage class behind broken deployments.
 *
 * A node without an ownerDocument still RENDERS, because rendering walks the parent, so nothing looks
 * wrong. Deployment export, however, walks documents - the pages and the ShadowDocument - so such a node
 * is silently left out of the export while every page referencing it keeps its reference. The next
 * import then yields empty page shells, and a page whose ROOT is such a node fails to import at all.
 *
 * How they came about: handleNewChild used to propagate the parent's ownerDocument unconditionally, so
 * attaching anything to a page-less parent CLEARED the child's document and its whole subtree's. One
 * page-less node was therefore contagious to everything later attached beneath it. That is fixed, but
 * the fix repairs nothing that already happened, which is what this class is for.
 *
 * DETACHED IS NOT ALWAYS DAMAGE. Structr's Recycle Bin is exactly a set of page-less, parentless nodes,
 * and adopting those would resurrect deleted content. The damage has a signature the Recycle Bin never
 * has: a node with no document of its own whose PARENT CHAIN reaches one. Hence three kinds, of which
 * only two are repaired.
 */
public class DetachedNodes {

	private static final Logger logger = LoggerFactory.getLogger(DetachedNodes.class);

	public enum Kind {

		/** no document, but an ancestor has one: repaired by adopting that document */
		ADOPTABLE,

		/** no document and no parent, but instances still point at it: it belongs in the ShadowDocument */
		ORPHANED_MASTER,

		/** no document, no parent, nothing refers to it: the Recycle Bin, reported and left alone */
		RECYCLE_BIN
	}

	/** one page-less node, with what it would take to fix it */
	public record Finding(String uuid, String type, String name, Kind kind, String documentUuid, String documentName) {

		public String describe() {

			return type + " " + uuid + (name == null ? "" : " \"" + name + "\"") + " [" + kind + "]"
				+ (documentName == null ? "" : " -> " + documentName);
		}
	}

	/**
	 * Every page-less DOM node, classified. Reads only.
	 *
	 * Pages and the ShadowDocument are documents themselves, so they are not candidates.
	 */
	public static List<Finding> scan(final App app) throws FrameworkException {

		final List<Finding> findings = new ArrayList<>();

		for (final NodeInterface node : app.nodeQuery(StructrTraits.DOM_NODE).getResultStream()) {

			if (node.is(StructrTraits.PAGE) || node.is(StructrTraits.SHADOW_DOCUMENT)) {
				continue;
			}

			final DOMNode domNode = node.as(DOMNode.class);

			if (domNode.getOwnerDocument() != null) {
				continue;
			}

			final Page document = documentOfAncestors(domNode);

			if (document != null) {

				findings.add(new Finding(node.getUuid(), node.getType(), node.getName(), Kind.ADOPTABLE,
					document.getUuid(), document.getName()));

			} else if (isReferencedMaster(domNode)) {

				findings.add(new Finding(node.getUuid(), node.getType(), node.getName(), Kind.ORPHANED_MASTER, null, null));

			} else {

				findings.add(new Finding(node.getUuid(), node.getType(), node.getName(), Kind.RECYCLE_BIN, null, null));
			}
		}

		return findings;
	}

	/**
	 * Repairs what can be repaired and answers how many nodes were adopted.
	 *
	 * ADOPTABLE nodes take the document of their nearest ancestor that has one; a detached subtree is
	 * fixed node by node rather than recursively, because the walk passes THROUGH page-less ancestors, so
	 * every node in it resolves to the same document on its own.
	 *
	 * ORPHANED_MASTER nodes go back to the ShadowDocument, which is where a shared component lives and
	 * where deployment export looks for it. RECYCLE_BIN nodes are never touched.
	 */
	public static int repair(final App app, final List<Finding> findings) throws FrameworkException {

		final Page shadow = CreateComponentCommand.getOrCreateHiddenDocument();
		int repaired      = 0;

		for (final Finding finding : findings) {

			final NodeInterface node = app.getNodeById(StructrTraits.DOM_NODE, finding.uuid());

			if (node == null) {
				continue;
			}

			switch (finding.kind()) {

				case ADOPTABLE -> {

					final NodeInterface document = app.getNodeById(StructrTraits.PAGE, finding.documentUuid());

					if (document != null) {
						node.as(DOMNode.class).setOwnerDocument(document.as(Page.class));
						repaired++;
					}
				}

				case ORPHANED_MASTER -> {

					node.as(DOMNode.class).setOwnerDocument(shadow);
					repaired++;
				}

				case RECYCLE_BIN -> { /* deliberately untouched: this is deleted content, not damage */ }
			}
		}

		return repaired;
	}

	/** how many of each kind, for a log line or a report */
	public static Map<Kind, Integer> countByKind(final List<Finding> findings) {

		final Map<Kind, Integer> counts = new LinkedHashMap<>();

		for (final Kind kind : Kind.values()) {
			counts.put(kind, 0);
		}

		for (final Finding finding : findings) {
			counts.merge(finding.kind(), 1, Integer::sum);
		}

		return counts;
	}

	/** the findings that mean data is missing from an export, i.e. everything except the Recycle Bin */
	public static List<Finding> damaged(final List<Finding> findings) {

		final List<Finding> out = new ArrayList<>();

		for (final Finding finding : findings) {

			if (!Kind.RECYCLE_BIN.equals(finding.kind())) {
				out.add(finding);
			}
		}

		return out;
	}

	/** scans and repairs in one go, logging what it did; the entry point for a caller with no report to write */
	public static int scanAndRepair(final String context) throws FrameworkException {

		final App app                  = StructrApp.getInstance();
		final List<Finding> findings   = scan(app);
		final List<Finding> damaged    = damaged(findings);

		if (damaged.isEmpty()) {
			return 0;
		}

		final int repaired = repair(app, damaged);

		logger.info("{}: adopted {} DOM node(s) that belonged to no document and would have been missing from a deployment export ({})",
			context, repaired, countByKind(damaged));

		for (final Finding finding : damaged) {
			logger.info("  {}", finding.describe());
		}

		return repaired;
	}

	/**
	 * The document an ancestor puts this node in, walking THROUGH page-less ancestors.
	 *
	 * An ancestor answers in one of two ways, and BOTH have to be asked. A page's own ownerDocument is
	 * null, because a Page IS a document rather than belonging to one, so asking only for ownerDocument
	 * walks straight past the page at the top of the chain and reports "nothing above has a document".
	 * On a real instance that misread a detached subtree hanging directly under a Page - the exact shape
	 * this class exists to repair - as deleted content, and left it alone.
	 */
	private static Page documentOfAncestors(final DOMNode node) throws FrameworkException {

		DOMNode current = node.getParent();

		while (current != null) {

			// the ancestor IS the document (a Page, or the ShadowDocument holding a component master)
			if (current.is(StructrTraits.PAGE)) {
				return current.as(Page.class);
			}

			final Page document = current.getOwnerDocument();

			if (document != null) {
				return document;
			}

			current = current.getParent();
		}

		return null;
	}

	/** a master that lost its ShadowDocument: instances still render it, so it must be exported */
	private static boolean isReferencedMaster(final DOMNode node) {

		final Iterable<DOMNode> instances = node.getSyncedNodes();

		return instances != null && instances.iterator().hasNext();
	}
}
