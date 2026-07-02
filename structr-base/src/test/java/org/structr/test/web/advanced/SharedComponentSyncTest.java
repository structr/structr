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
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.testng.annotations.Test;

import static org.structr.web.entity.dom.DOMNode.SHARED_COMPONENT_SYNC_MODE;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Tests for the unified, syncMode-aware shared-component property sync that lives in
 * DOMNode.onModification (DOMNode.syncSharedComponentProperties). A change to a shared
 * Content node propagates to its synced peers depending on the sync mode carried on the
 * SecurityContext: ALL (every peer), BY_VALUE (peers that still hold the previous value),
 * NONE (this node only); absent mode defaults to ALL.
 */
public class SharedComponentSyncTest extends StructrUiTest {

	private PropertyKey<String> contentKey() {
		return Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_PROPERTY);
	}

	private PropertyKey<NodeInterface> sharedComponentKey() {
		return Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHARED_COMPONENT_PROPERTY);
	}

	private PropertyKey<Boolean> visibleToPublicKey() {
		return Traits.of(StructrTraits.CONTENT).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY);
	}

	/**
	 * Creates a shared "original" Content and two synced instances (each instance's
	 * sharedComponent points at the original, which makes them the original's syncedNodes).
	 * All nodes are created and wired in a single transaction, so this is creation, not
	 * modification, and no sync fires during setup. Returns [originalId, inst1Id, inst2Id].
	 */
	private String[] createSyncTriple(final String originalContent, final String inst1Content, final String inst2Content) throws FrameworkException {

		final String[] ids = new String[3];

		try (final Tx tx = app.tx()) {

			final NodeInterface original = app.create(StructrTraits.CONTENT, new PropertyMap(contentKey(), originalContent));
			final NodeInterface inst1    = app.create(StructrTraits.CONTENT, new PropertyMap(contentKey(), inst1Content));
			final NodeInterface inst2    = app.create(StructrTraits.CONTENT, new PropertyMap(contentKey(), inst2Content));

			inst1.setProperty(sharedComponentKey(), original);
			inst2.setProperty(sharedComponentKey(), original);

			ids[0] = original.getUuid();
			ids[1] = inst1.getUuid();
			ids[2] = inst2.getUuid();

			tx.success();
		}

		return ids;
	}

	/** Sets a property on a node in its own transaction, optionally carrying a sync mode. */
	private <T> void edit(final String id, final PropertyKey<T> key, final T value, final SHARED_COMPONENT_SYNC_MODE mode) throws FrameworkException {

		try (final Tx tx = app.tx()) {

			if (mode != null) {
				securityContext.setAttribute(DOMNode.SHARED_COMPONENT_SYNC_MODE_ATTRIBUTE, mode);
			}

			app.getNodeById(StructrTraits.CONTENT, id).setProperty(key, value);

			tx.success();

		} finally {

			// never let the mode leak into the next test (the test SecurityContext is reused)
			securityContext.removeAttribute(DOMNode.SHARED_COMPONENT_SYNC_MODE_ATTRIBUTE);
		}
	}

	private String contentOf(final String id) throws FrameworkException {
		try (final Tx tx = app.tx()) {
			final String c = app.getNodeById(StructrTraits.CONTENT, id).getProperty(contentKey());
			tx.success();
			return c;
		}
	}

	private boolean visibleToPublicOf(final String id) throws FrameworkException {
		try (final Tx tx = app.tx()) {
			final Boolean v = app.getNodeById(StructrTraits.CONTENT, id).getProperty(visibleToPublicKey());
			tx.success();
			return Boolean.TRUE.equals(v);
		}
	}

	@Test
	public void testDefaultModeIsAll() {

		try {
			final String[] ids = createSyncTriple("V0", "V0", "V0");

			// no mode on the context -> historic default is ALL
			edit(ids[0], contentKey(), "EDITED", null);

			assertEquals("original should be edited",         "EDITED", contentOf(ids[0]));
			assertEquals("instance 1 should sync (default ALL)", "EDITED", contentOf(ids[1]));
			assertEquals("instance 2 should sync (default ALL)", "EDITED", contentOf(ids[2]));

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAllSync() {

		try {
			final String[] ids = createSyncTriple("V0", "V0", "V0");

			edit(ids[0], contentKey(), "EDITED", SHARED_COMPONENT_SYNC_MODE.ALL);

			assertEquals("original",   "EDITED", contentOf(ids[0]));
			assertEquals("instance 1", "EDITED", contentOf(ids[1]));
			assertEquals("instance 2", "EDITED", contentOf(ids[2]));

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testByValueSync() {

		try {
			// instance 1 still holds the original's previous value, instance 2 diverges
			final String[] ids = createSyncTriple("V0", "V0", "DIFFERENT");

			edit(ids[0], contentKey(), "EDITED", SHARED_COMPONENT_SYNC_MODE.BY_VALUE);

			assertEquals("original is edited",                       "EDITED",    contentOf(ids[0]));
			assertEquals("instance 1 (matched previous value) syncs", "EDITED",    contentOf(ids[1]));
			assertEquals("instance 2 (different value) is untouched", "DIFFERENT", contentOf(ids[2]));

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testNoneSync() {

		try {
			final String[] ids = createSyncTriple("V0", "V0", "V0");

			edit(ids[0], contentKey(), "EDITED", SHARED_COMPONENT_SYNC_MODE.NONE);

			assertEquals("original is edited",        "EDITED", contentOf(ids[0]));
			assertEquals("instance 1 is NOT synced",  "V0",     contentOf(ids[1]));
			assertEquals("instance 2 is NOT synced",  "V0",     contentOf(ids[2]));

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testBidirectionalSyncFromInstance() {

		try {
			// editing an INSTANCE must reach the original AND the sibling (up then down,
			// via the modification-queue fixpoint cascade)
			final String[] ids = createSyncTriple("V0", "V0", "V0");

			edit(ids[1], contentKey(), "EDITED", SHARED_COMPONENT_SYNC_MODE.ALL);

			assertEquals("edited instance",          "EDITED", contentOf(ids[1]));
			assertEquals("original (synced upward)", "EDITED", contentOf(ids[0]));
			assertEquals("sibling (synced via cascade)", "EDITED", contentOf(ids[2]));

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testDenylistedPropertyIsNotSynced() {

		try {
			final String[] ids = createSyncTriple("V0", "V0", "V0");

			// visibility is on the denylist: even with ALL, it must NOT propagate to peers
			edit(ids[0], visibleToPublicKey(), Boolean.TRUE, SHARED_COMPONENT_SYNC_MODE.ALL);

			assertEquals("original visibility changed",          true,  visibleToPublicOf(ids[0]));
			assertEquals("instance 1 visibility NOT propagated", false, visibleToPublicOf(ids[1]));
			assertEquals("instance 2 visibility NOT propagated", false, visibleToPublicOf(ids[2]));

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}
}
