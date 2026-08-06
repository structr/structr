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
package org.structr.test.web.advanced;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.maintenance.DetachedNodes;
import org.structr.websocket.command.CloneComponentCommand;
import org.structr.websocket.command.CreateComponentCommand;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * The repair behind the detachedNodes maintenance command, the startup migration and the export check.
 *
 * The distinction that matters here is between damage and deleted content. Structr's Recycle Bin is a
 * set of page-less, parentless nodes, so a repair that adopts everything without a document would
 * resurrect what somebody deleted. The damage looks different - a node with no document whose parent
 * chain still reaches one - and only that (plus a master that lost the ShadowDocument) is repaired.
 */
public class DetachedNodesTest extends StructrUiTest {

	private List<DetachedNodes.Finding> scan() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final List<DetachedNodes.Finding> findings = DetachedNodes.scan(app);
			tx.success();

			return findings;
		}
	}

	private int repair() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final int repaired = DetachedNodes.scanAndRepair("test");
			tx.success();

			return repaired;
		}
	}

	private DetachedNodes.Finding findingFor(final String uuid) throws FrameworkException {

		for (final DetachedNodes.Finding finding : scan()) {

			if (finding.uuid().equals(uuid)) {
				return finding;
			}
		}

		return null;
	}

	/* every read happens INSIDE its transaction: handing a node out of one and reading a property
	   afterwards throws NotInTransactionException, so these answer with values, not nodes */

	private boolean exists(final String uuid) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final boolean found = app.getNodeById(StructrTraits.DOM_NODE, uuid) != null;
			tx.success();

			return found;
		}
	}

	private String documentNameOf(final String uuid) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Page document = app.getNodeById(StructrTraits.DOM_NODE, uuid).as(DOMNode.class).getOwnerDocument();
			final String name   = document == null ? null : document.getName();

			tx.success();

			return name;
		}
	}

	private boolean belongsToTheShadowDocument(final String uuid) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Page document = app.getNodeById(StructrTraits.DOM_NODE, uuid).as(DOMNode.class).getOwnerDocument();
			final boolean shadow = document != null && document.is(StructrTraits.SHADOW_DOCUMENT);

			tx.success();

			return shadow;
		}
	}

	@Test
	public void aHealthyInstanceHasNothingToRepair() throws FrameworkException {

		try (final Tx tx = app.tx()) {
			Page.createSimplePage(securityContext, "home");
			tx.success();
		}

		assertTrue("a healthy page must produce no findings", scan().isEmpty());
		assertEquals("and nothing to repair", 0, repair());
	}

	@Test
	public void aNodeWhoseParentChainReachesAPageIsAdopted() throws FrameworkException {

		final String divId;

		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "home");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			div.setOwnerDocument(null);
			divId = div.getUuid();

			tx.success();
		}

		assertEquals(DetachedNodes.Kind.ADOPTABLE, findingFor(divId).kind());
		assertEquals("one node repaired", 1, repair());

		assertEquals("it belongs to its page again", "home", documentNameOf(divId));
	}

	/** a whole detached subtree: the walk passes THROUGH page-less ancestors, so every node resolves */
	@Test
	public void anEntireDetachedSubtreeIsAdopted() throws FrameworkException {

		final String outerId, innerId, textId;

		try (final Tx tx = app.tx()) {

			final Page page        = Page.createSimplePage(securityContext, "home");
			final DOMNode body     = page.getElementsByTagName("body").get(0);
			final DOMElement outer = page.createElement("section");
			final DOMElement inner = page.createElement("p");
			final DOMNode text     = page.createTextNode("content");

			body.appendChild(outer);
			outer.appendChild(inner);
			inner.appendChild(text);

			// what the old handleNewChild did to a whole subtree at once
			outer.setOwnerDocument(null);
			inner.setOwnerDocument(null);
			text.setOwnerDocument(null);

			outerId = outer.getUuid();
			innerId = inner.getUuid();
			textId  = text.getUuid();

			tx.success();
		}

		assertEquals("three nodes to repair", 3, scan().size());
		assertEquals(3, repair());

		for (final String uuid : List.of(outerId, innerId, textId)) {
			assertEquals("every node in the subtree belongs to the page again: " + uuid, "home", documentNameOf(uuid));
		}
	}

	@Test
	public void aMasterThatLostTheShadowDocumentGoesBackToIt() throws FrameworkException {

		final String masterId;

		try (final Tx tx = app.tx()) {

			final Page page       = Page.createSimplePage(securityContext, "home");
			final DOMNode body    = page.getElementsByTagName("body").get(0);
			final DOMElement head = page.createElement("header");

			body.appendChild(head);

			final DOMNode master = new CreateComponentCommand().create(head);

			master.setProperty(Traits.of(StructrTraits.DOM_NODE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "Site Header");

			// a second page carries an instance, so the master is still in use
			final Page other = Page.createSimplePage(securityContext, "other");
			CloneComponentCommand.cloneComponent(master, other.getElementsByTagName("body").get(0));

			// the state that broke the deployment: the master itself belongs to nothing
			master.setOwnerDocument(null);
			masterId = master.getUuid();

			tx.success();
		}

		assertEquals(DetachedNodes.Kind.ORPHANED_MASTER, findingFor(masterId).kind());
		assertTrue("the master is repaired", repair() > 0);


		assertTrue("the master must belong to the ShadowDocument again", belongsToTheShadowDocument(masterId));
	}

	/**
	 * The safety case. A page-less, parentless node that nothing refers to is how DELETED content looks
	 * in Structr, so a repair that adopted it would put deleted content back into the app and into the
	 * next deployment export.
	 */
	@Test
	public void theRecycleBinIsReportedButNeverTouched() throws FrameworkException {

		final String orphanId;

		try (final Tx tx = app.tx()) {

			final Page page      = Page.createSimplePage(securityContext, "home");
			final DOMElement div = page.createElement("div");

			div.setOwnerDocument(null);   // never attached, belongs to nothing: the Recycle Bin
			orphanId = div.getUuid();

			tx.success();
		}

		assertEquals(DetachedNodes.Kind.RECYCLE_BIN, findingFor(orphanId).kind());
		assertEquals("deleted content must not be adopted", 0, repair());

		assertNull("it must still belong to nothing", documentNameOf(orphanId));
		assertTrue("and it must still exist - a repair deletes nothing", exists(orphanId));
	}

	@Test
	public void repairIsIdempotent() throws FrameworkException {

		try (final Tx tx = app.tx()) {

			final Page page   = Page.createSimplePage(securityContext, "home");
			final DOMNode div = page.getElementsByTagName("div").get(0);

			div.setOwnerDocument(null);
			tx.success();
		}

		assertEquals("first run repairs", 1, repair());
		assertEquals("second run has nothing left to do", 0, repair());
	}
}
