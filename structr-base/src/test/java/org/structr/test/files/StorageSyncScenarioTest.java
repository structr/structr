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
package org.structr.test.files;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.files.sync.StorageSyncService;
import org.structr.storage.sync.SyncDirection;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.sync.ExternalChangeEvent;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.VanishPolicy;
import org.structr.storage.sync.VirtualChangeEvent;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.traits.definitions.FolderTraitDefinition;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Storage sync behaviour driven through a ScriptedStorageProvider, for the cases a real backend cannot
 * produce on demand: failures at a chosen point, entries in a chosen order, scans held mid-way.
 */
public class StorageSyncScenarioTest extends SyncTestBase {

	private static final long MODIFIED               = 1_700_000_000_000L;
	private static final Map<String, String> BOTH    = Map.of(SyncDirection.DIRECTION_KEY, "both");
	private static final Map<String, String> DELETE  = Map.of(VanishPolicy.POLICY_KEY, VanishPolicy.DELETE.value());

	@Test
	public void testPartialScanNeverDeletes() {
		assertPartialScansDoNotReconcile("partialDelete", VanishPolicy.DELETE, false);
	}

	@Test
	public void testPartialScanNeverMarks() {
		assertPartialScansDoNotReconcile("partialMark", VanishPolicy.READONLY, false);
	}

	@Test
	public void testScanFailingBeforeTheFirstEntryNeverDeletes() {
		assertPartialScansDoNotReconcile("failingDelete", VanishPolicy.DELETE, true);
	}

	/**
	 * A scan that does not finish cannot tell an entry that vanished from one it simply did not reach.
	 * Two entries are imported, then every scan hands out only the first and fails; the second must
	 * survive those scans untouched, and the scan time must not advance. Lifting the failure must then
	 * reconcile it, which proves the test would have seen a wrongful reconciliation.
	 */
	private void assertPartialScansDoNotReconcile(final String name, final VanishPolicy policy, final boolean failBeforeFirstEntry) {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario(name)
			.supportsWatching(false)
			.entries(ExternalEntry.file("kept.txt", 10L, MODIFIED), ExternalEntry.file("unreached.txt", 10L, MODIFIED));

		mountScripted(name, scenario, false, 1, Map.of(VanishPolicy.POLICY_KEY, policy.value()));

		waitForNodeAtPath(StructrTraits.FILE, "/" + name + "/unreached.txt");
		awaitScanAfter(name, 0L);

		final Long scannedBefore = getLastScanned(name);

		// from now on every scan breaks off before it gets to the second entry
		scenario.entries(ExternalEntry.file("kept.txt", 10L, MODIFIED));

		if (failBeforeFirstEntry) {

			scenario.failImmediately(true);

		} else {

			scenario.failAfter(1);
		}

		final int enumerationsBefore = scenario.enumerations();

		waitFor("Two failing scans should have run", () -> scenario.enumerations() >= enumerationsBefore + 2);
		awaitIdle();

		assertNotNull("An entry a failed scan did not reach must not be deleted", getNode("/" + name + "/unreached.txt"));
		assertFalse("An entry a failed scan did not reach must not be marked",   isExternallyDeleted("unreached.txt"));
		assertEquals("A failed scan must not advance the scan time", scannedBefore, getLastScanned(name));

		// a complete scan that does not list the entry does reconcile it
		scenario.failImmediately(false).failAfter(-1);

		if (VanishPolicy.DELETE.equals(policy)) {

			waitFor("A complete scan should delete the vanished entry", () -> getNode("/" + name + "/unreached.txt") == null);

		} else {

			waitFor("A complete scan should mark the vanished entry", () -> isExternallyDeleted("unreached.txt"));
		}
	}

	@Test
	public void testUnlinkingTheConfigurationDetachesTheMount() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("unlink").entries(ExternalEntry.file("a.txt", 10L, MODIFIED));
		final String rootUuid                           = mountAndAwaitAttach("unlink", scenario);

		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.FOLDER, rootUuid).setProperty(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), null);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		waitFor("Unlinking the configuration should close the synchronizer", () -> scenario.closes() >= 1);
		assertFalse("A folder without a configuration should no longer be synchronized", service().isSynchronized(rootUuid));
	}

	@Test
	public void testDeletingTheConfigurationDetachesTheMount() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("deleteConfig").entries(ExternalEntry.file("a.txt", 10L, MODIFIED));
		final String rootUuid                           = mountAndAwaitAttach("deleteConfig", scenario);

		try (final Tx tx = app.tx()) {

			app.delete(app.getNodeById(StructrTraits.FOLDER, rootUuid).as(AbstractFile.class).getStorageConfiguration());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		waitFor("Deleting the configuration should close the synchronizer", () -> scenario.closes() >= 1);
		assertFalse("A mount whose configuration is gone should no longer be synchronized", service().isSynchronized(rootUuid));
	}

	@Test
	public void testChangedDirectionReattachesWithTheNewBehaviour() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("direction").entries(ExternalEntry.file("a.txt", 10L, MODIFIED));
		final String rootUuid                           = mountAndAwaitAttach("direction", scenario);

		waitFor("The inbound mount should be watched", () -> scenario.watchStarts() == 1);
		awaitIdle();

		final int enumerationsBefore = scenario.enumerations();

		// outbound only: the re-attached mount must neither watch nor scan
		try (final Tx tx = app.tx()) {

			app.getNodeById(StructrTraits.FOLDER, rootUuid).as(AbstractFile.class).getStorageConfiguration().addEntry(SyncDirection.DIRECTION_KEY, "out");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		waitFor("A changed direction should re-attach the mount", () -> scenario.synchronizersCreated() == 2 && scenario.closes() == 1);
		awaitIdle();

		assertTrue("The re-attached mount should still be synchronized", service().isSynchronized(rootUuid));
		assertEquals("An outbound-only mount must not be watched", 1, scenario.watchStarts());
		assertEquals("An outbound-only mount must not be scanned", enumerationsBefore, scenario.enumerations());
	}

	@Test
	public void testChangesWithinOneTransactionAreSentAsTheirNetEffect() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("coalesce").entries(
			ExternalEntry.directory("a", MODIFIED),
			ExternalEntry.directory("b", MODIFIED),
			ExternalEntry.file("x.txt", 10L, MODIFIED),
			ExternalEntry.file("y.txt", 10L, MODIFIED)
		);

		mountAndAwaitAttach("coalesce", scenario, BOTH);

		final NodeInterface root = getNodeAtPath("/coalesce");
		final NodeInterface a    = getNodeAtPath("/coalesce/a");
		final NodeInterface b    = getNodeAtPath("/coalesce/b");
		final NodeInterface x    = getNodeAtPath("/coalesce/x.txt");
		final NodeInterface y    = getNodeAtPath("/coalesce/y.txt");

		// created, then renamed: created under the final name
		assertOutbound(scenario, () -> setName(createFolder("new", root), "renamed"), "CREATED renamed");

		// created, then deleted: still a delete, because content may already have reached the storage
		// through the provider's write path, and only what exists there is removed
		assertOutbound(scenario, () -> app.delete(createFolder("temporary", root)), "DELETED temporary");

		// moved twice: one move from the original location to the final one
		assertOutbound(scenario, () -> {
			setParent(x, a);
			setParent(x, b);
		}, "MOVED x.txt -> b/x.txt");

		// moved, then deleted: deleted at the original location
		assertOutbound(scenario, () -> {
			setParent(y, a);
			app.delete(y);
		}, "DELETED y.txt");

		// renamed, then deleted: deleted under the original name
		assertOutbound(scenario, () -> {
			setName(x, "z.txt");
			app.delete(x);
		}, "DELETED b/x.txt");
	}

	@Test
	public void testMoveByParentIdIsSentAsMove() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("parentId").entries(
			ExternalEntry.directory("a", MODIFIED),
			ExternalEntry.file("x.txt", 10L, MODIFIED)
		);

		mountAndAwaitAttach("parentId", scenario, BOTH);

		final NodeInterface a = getNodeAtPath("/parentId/a");
		final NodeInterface x = getNodeAtPath("/parentId/x.txt");

		assertOutbound(scenario, () -> x.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PARENT_ID_PROPERTY), a.getUuid()), "MOVED x.txt -> a/x.txt");
	}

	@Test
	public void testRecursiveFolderDeleteSendsEveryNodeDeepestFirst() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("recursive").entries(
			ExternalEntry.directory("s", MODIFIED),
			ExternalEntry.file("s/y.txt", 10L, MODIFIED),
			ExternalEntry.directory("s/t", MODIFIED),
			ExternalEntry.file("s/t/x.txt", 10L, MODIFIED)
		);

		mountAndAwaitAttach("recursive", scenario, BOTH);

		final NodeInterface s = getNodeAtPath("/recursive/s");

		assertNotNull("The nested file should have been imported", getNodeAtPath("/recursive/s/t/x.txt"));

		assertOutbound(scenario, () -> deleteRecursively(s), "DELETED s/t/x.txt", "DELETED s/y.txt", "DELETED s/t", "DELETED s");
	}

	@Test
	public void testMoveBetweenMountsIsDeletedInOneAndCreatedInTheOther() {

		final ScriptedStorageProvider.Scenario source = ScriptedStorageProvider.scenario("source").entries(ExternalEntry.file("x.txt", 10L, MODIFIED));
		final ScriptedStorageProvider.Scenario target = ScriptedStorageProvider.scenario("target").entries(ExternalEntry.directory("in", MODIFIED));

		mountAndAwaitAttach("source", source, BOTH);
		mountAndAwaitAttach("target", target, BOTH);

		final NodeInterface x  = getNodeAtPath("/source/x.txt");
		final NodeInterface in = getNodeAtPath("/target/in");

		target.outbound().clear();

		assertOutbound(source, () -> setParent(x, in), "DELETED x.txt");
		assertEquals("The target mount should receive the moved file as created", List.of("CREATED in/x.txt"), describeOutbound(target));
	}

	@Test
	public void testMovingOrRenamingTheSyncRootOrItsAncestorsSendsNothing() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("relocated").entries(ExternalEntry.file("x.txt", 10L, MODIFIED));

		mountAndAwaitAttach("relocated", scenario, BOTH);

		final NodeInterface root = getNodeAtPath("/relocated");
		final NodeInterface x    = getNodeAtPath("/relocated/x.txt");

		assertOutbound(scenario, () -> setName(root, "renamedRoot"));
		assertOutbound(scenario, () -> setParent(root, createFolder("outer", null)));
		assertOutbound(scenario, () -> setName(getNodeAtPath("/outer"), "outer2"));

		// paths stay relative to the root wherever it is now
		assertOutbound(scenario, () -> setName(x, "y.txt"), "MOVED x.txt -> y.txt");
		assertNotNull("The file should be below the relocated root", getNodeAtPath("/outer2/renamedRoot/y.txt"));
	}

	@Test
	public void testExternalFileCreatedAndDeletedWithinTheDebounceIsNeverHandled() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("transient");

		mountAndAwaitAttach("transient", scenario, DELETE);

		final long handledBefore = handledEvents();

		scenario.emit(ExternalChangeEvent.created(ExternalEntry.file("new.txt", 10L, MODIFIED)));
		scenario.emit(ExternalChangeEvent.deleted(ExternalEntry.file("new.txt", 10L, MODIFIED)));

		awaitIdle();

		assertEquals("A file that came and went before the debounce elapsed should not be handled at all", handledBefore, handledEvents());
		assertNull("A file that came and went should not exist", getNodeAtPath("/transient/new.txt"));
	}

	@Test
	public void testAtomicSaveKeepsTheNode() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("atomicSave").entries(ExternalEntry.file("x.txt", 10L, MODIFIED));

		mountAndAwaitAttach("atomicSave", scenario, DELETE);

		final String uuid = uuidAt("/atomicSave/x.txt");

		// an editor writing a temporary file and renaming it over the original
		scenario.emit(ExternalChangeEvent.deleted(ExternalEntry.file("x.txt", 10L, MODIFIED)));
		scenario.emit(ExternalChangeEvent.created(ExternalEntry.file("x.txt", 20L, MODIFIED + 1000)));

		awaitIdle();

		assertEquals("An atomically saved file should keep its node", uuid, uuidAt("/atomicSave/x.txt"));
	}

	@Test
	public void testModificationBurstIsHandledOnce() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("burst").entries(ExternalEntry.file("x.txt", 10L, MODIFIED));

		mountAndAwaitAttach("burst", scenario, DELETE);

		final long handledBefore = handledEvents();

		for (int i = 1; i <= 5; i++) {

			scenario.emit(ExternalChangeEvent.modified(ExternalEntry.file("x.txt", 10L + i, MODIFIED + i)));
		}

		awaitHandledEventsBeyond(handledBefore);
		awaitIdle();

		assertEquals("A burst of modifications should be handled as one", handledBefore + 1, handledEvents());
	}

	@Test
	public void testFileDeletedRecreatedAndDeletedAgainIsDeleted() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("flapping").entries(ExternalEntry.file("x.txt", 10L, MODIFIED));

		mountAndAwaitAttach("flapping", scenario, DELETE);

		scenario.emit(ExternalChangeEvent.deleted(ExternalEntry.file("x.txt", 10L, MODIFIED)));
		scenario.emit(ExternalChangeEvent.created(ExternalEntry.file("x.txt", 20L, MODIFIED + 1000)));
		scenario.emit(ExternalChangeEvent.deleted(ExternalEntry.file("x.txt", 20L, MODIFIED + 1000)));

		waitFor("A file that ends up deleted should be deleted", () -> getNodeAtPath("/flapping/x.txt") == null);
	}

	@Test
	public void testFileMovedAndThenDeletedIsDeleted() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("movedAway").entries(ExternalEntry.file("a.txt", 10L, MODIFIED));

		mountAndAwaitAttach("movedAway", scenario, DELETE);

		scenario.emit(ExternalChangeEvent.moved("a.txt", ExternalEntry.file("b.txt", 10L, MODIFIED)));
		scenario.emit(ExternalChangeEvent.deleted(ExternalEntry.file("b.txt", 10L, MODIFIED)));

		waitFor("A file that was moved and then deleted should be deleted", () -> getNodeAtPath("/movedAway/a.txt") == null);
		assertNull("A file that was moved and then deleted should not exist at its new path", getNodeAtPath("/movedAway/b.txt"));
	}

	/**
	 * A name containing NUL is rejected by validation, which runs when the transaction holding the
	 * event commits.
	 */
	@Test
	public void testOneInvalidEventDoesNotCostTheOthersOrStallTheService() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("invalid");

		mountAndAwaitAttach("invalid", scenario, DELETE);

		scenario.emit(ExternalChangeEvent.created(ExternalEntry.file("bad\0.txt", 10L, MODIFIED)));
		scenario.emit(ExternalChangeEvent.created(ExternalEntry.file("good.txt", 10L, MODIFIED)));

		waitForNodeAtPath(StructrTraits.FILE, "/invalid/good.txt");

		scenario.emit(ExternalChangeEvent.created(ExternalEntry.file("later.txt", 10L, MODIFIED)));

		waitForNodeAtPath(StructrTraits.FILE, "/invalid/later.txt");
	}

	/**
	 * A name containing NUL is rejected by validation when the batch of discovered entries holding it
	 * commits. Each scan is requested explicitly, so that the test knows which scan it waits for.
	 */
	@Test
	public void testOneInvalidEntryDoesNotCostItsBatchOrTheReconciliation() {

		final List<ExternalEntry> entries = new ArrayList<>();

		for (int i = 0; i < 1500; i++) {

			entries.add(ExternalEntry.file(i == 500 ? "bad\0.txt" : "file" + i + ".txt", 10L, MODIFIED));
		}

		final List<ExternalEntry> initial = new ArrayList<>(entries.subList(0, 500));

		initial.add(ExternalEntry.file("vanishing.txt", 10L, MODIFIED));

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("invalidEntry").entries(initial);

		mountAndAwaitAttach("invalidEntry", scenario, DELETE);

		assertNotNull("The first scan should have imported the entry that vanishes later", getNodeAtPath("/invalidEntry/vanishing.txt"));

		final long before = System.currentTimeMillis();

		scenario.entries(entries);
		scenario.requestFullScan();

		awaitScanAfter("invalidEntry", before);

		assertNotNull("An entry in the same batch as the invalid one should be imported", getNodeAtPath("/invalidEntry/file499.txt"));
		assertNotNull("An entry in the same batch as the invalid one should be imported", getNodeAtPath("/invalidEntry/file999.txt"));
		assertNotNull("An entry in a later batch should be imported",                     getNodeAtPath("/invalidEntry/file1499.txt"));
		assertNull("A complete scan should still reconcile",                              getNodeAtPath("/invalidEntry/vanishing.txt"));
	}

	@Test
	public void testInboundEventsAtTheNestedRootsOwnPathLeaveItAlone() {

		final ScriptedStorageProvider.Scenario outer = ScriptedStorageProvider.scenario("nestedOuter");

		ScriptedStorageProvider.scenario("nestedInner");

		// created together, so that no scan of the outer mount can create a plain "inner" folder first
		try (final Tx tx = app.tx()) {

			final NodeInterface root = mountFolder("nestedOuter", StorageProviderFactory.createConfig("scripted-nestedOuter", ScriptedStorageProvider.class,
				Map.of(ScriptedStorageProvider.SCENARIO_KEY, "nestedOuter", VanishPolicy.POLICY_KEY, VanishPolicy.DELETE.value())), true, null);

			app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "inner"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), root),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY),
					StorageProviderFactory.createConfig("scripted-nestedInner", ScriptedStorageProvider.class, Map.of(ScriptedStorageProvider.SCENARIO_KEY, "nestedInner")))
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		waitFor("The outer mount should be watched", () -> outer.watchStarts() >= 1);
		awaitScanAfter("nestedOuter", 0L);

		final String innerUuid = uuidAt("/nestedOuter/inner");
		final long before      = handledEvents();

		outer.emit(ExternalChangeEvent.deleted(ExternalEntry.directory("inner", MODIFIED)));
		outer.emit(ExternalChangeEvent.moved("inner", ExternalEntry.directory("elsewhere", MODIFIED)));

		awaitHandledEventsBeyond(before);
		awaitIdle();

		assertEquals("A nested root is governed by its own configuration, not by the outer mount's events", innerUuid, uuidAt("/nestedOuter/inner"));
		assertFalse("The move's destination should be imported as a new entry, not be the nested root", innerUuid.equals(uuidAt("/nestedOuter/elsewhere")));
	}

	@Test
	public void testInboundEventsNeverDeleteOrMoveTheSyncRoot() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("rootEvents").entries(ExternalEntry.file("a.txt", 10L, MODIFIED));
		final String rootUuid                           = mountAndAwaitAttach("rootEvents", scenario, DELETE);
		final long before                               = handledEvents();

		scenario.emit(ExternalChangeEvent.deleted(ExternalEntry.directory("", MODIFIED)));
		scenario.emit(ExternalChangeEvent.deleted(ExternalEntry.byUuid(rootUuid, true, null, MODIFIED)));
		scenario.emit(ExternalChangeEvent.modified(ExternalEntry.byUuidAndPath(rootUuid, "elsewhere", true, null, MODIFIED)));

		awaitHandledEventsBeyond(before);
		awaitIdle();

		assertEquals("The sync root should be neither deleted nor moved by events of its own mount", rootUuid, uuidAt("/rootEvents"));
		assertFalse("The sync root should not be marked as vanished", isExternallyDeleted("rootEvents"));
	}

	@Test
	public void testEntryAddressingANodeOfAnotherMountByUuidIsIgnored() {

		final ScriptedStorageProvider.Scenario other    = ScriptedStorageProvider.scenario("foreignOther").entries(ExternalEntry.file("own.txt", 10L, MODIFIED));
		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("foreignThis");

		mountAndAwaitAttach("foreignOther", other);
		mountAndAwaitAttach("foreignThis", scenario, DELETE);

		final String foreignUuid = uuidAt("/foreignOther/own.txt");
		final long before        = handledEvents();

		scenario.emit(ExternalChangeEvent.modified(ExternalEntry.byUuidAndPath(foreignUuid, "stolen.txt", false, 10L, MODIFIED)));
		scenario.emit(ExternalChangeEvent.deleted(ExternalEntry.byUuid(foreignUuid, false, null, MODIFIED)));

		awaitHandledEventsBeyond(before);
		awaitIdle();

		assertEquals("A node governed by another mount should stay where it is", foreignUuid, uuidAt("/foreignOther/own.txt"));
		assertNull("A node governed by another mount should not be pulled into this one", getNodeAtPath("/foreignThis/stolen.txt"));
	}

	@Test
	public void testReconciliationKeepsStructrNodesAndTheFoldersAboveThem() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("kept").entries(
			ExternalEntry.directory("a", MODIFIED),
			ExternalEntry.file("a/x.txt", 10L, MODIFIED),
			ExternalEntry.directory("b", MODIFIED),
			ExternalEntry.file("b/y.txt", 10L, MODIFIED)
		);

		mountAndAwaitAttach("kept", scenario, Map.of(VanishPolicy.POLICY_KEY, VanishPolicy.READONLY.value()));

		// created in Structr, so not external: it has no counterpart a scan could miss
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "local.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/kept/a"))
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final long before = System.currentTimeMillis();

		scenario.entries();
		scenario.requestFullScan();

		awaitScanAfter("kept", before);

		assertTrue("A vanished entry should be marked",                          isExternallyDeleted("x.txt"));
		assertTrue("A vanished folder with nothing else in it should be marked", isExternallyDeleted("b"));
		assertNotNull("A node created in Structr should be kept",                getNodeAtPath("/kept/a/local.txt"));
		assertFalse("A vanished folder holding a kept node should not be marked", isExternallyDeleted("a"));
	}

	@Test
	public void testScanLargerThanOneBatchReconcilesExactlyWhatVanished() {

		final List<ExternalEntry> entries = new ArrayList<>();

		for (int i = 0; i < 1500; i++) {

			entries.add(ExternalEntry.file("file" + i + ".txt", 10L, MODIFIED));
		}

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("batches").entries(entries);

		mountAndAwaitAttach("batches", scenario, DELETE);

		assertNotNull("The last entry should have been imported", getNodeAtPath("/batches/file1499.txt"));

		final List<ExternalEntry> remaining = new ArrayList<>(entries);

		remaining.remove(1100);
		remaining.remove(10);

		final long before = System.currentTimeMillis();

		scenario.entries(remaining);
		scenario.requestFullScan();

		awaitScanAfter("batches", before);

		assertNull("An entry missing from the first batch should be deleted", getNodeAtPath("/batches/file10.txt"));
		assertNull("An entry missing from the last batch should be deleted",  getNodeAtPath("/batches/file1100.txt"));
		assertNotNull("An entry at the end of a batch should be kept",         getNodeAtPath("/batches/file999.txt"));
		assertNotNull("An entry at the start of a batch should be kept",       getNodeAtPath("/batches/file1000.txt"));
		assertNotNull("The last entry should be kept",                         getNodeAtPath("/batches/file1499.txt"));
	}

	@Test
	public void testConfiguredTargetTypesAndTheFallbackForAnUnknownOne() {

		ScriptedStorageProvider.scenario("types").entries(ExternalEntry.directory("d", MODIFIED), ExternalEntry.file("d/picture.png", 10L, MODIFIED));
		ScriptedStorageProvider.scenario("unknownTypes").entries(ExternalEntry.directory("d", MODIFIED), ExternalEntry.file("d/x.txt", 10L, MODIFIED));

		try (final Tx tx = app.tx()) {

			final Traits traits = Traits.of(StructrTraits.FOLDER);

			final NodeInterface typed = mountFolder("types", StorageProviderFactory.createConfig("scripted-types", ScriptedStorageProvider.class, Map.of(ScriptedStorageProvider.SCENARIO_KEY, "types")), false, null);

			typed.setProperty(traits.key(FolderTraitDefinition.MOUNT_TARGET_FILE_TYPE_PROPERTY), StructrTraits.IMAGE);

			final NodeInterface unknown = mountFolder("unknownTypes", StorageProviderFactory.createConfig("scripted-unknownTypes", ScriptedStorageProvider.class, Map.of(ScriptedStorageProvider.SCENARIO_KEY, "unknownTypes")), false, null);

			unknown.setProperty(traits.key(FolderTraitDefinition.MOUNT_TARGET_FILE_TYPE_PROPERTY),   "NoSuchType");
			unknown.setProperty(traits.key(FolderTraitDefinition.MOUNT_TARGET_FOLDER_TYPE_PROPERTY), StructrTraits.FILE);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		awaitScanAfter("types", 0L);
		awaitScanAfter("unknownTypes", 0L);

		assertEquals("Files should be created with the configured type",             StructrTraits.IMAGE,  typeAt("/types/d/picture.png"));
		assertEquals("Folders should keep the default type when none is configured", StructrTraits.FOLDER, typeAt("/types/d"));
		assertEquals("An unknown file type should fall back to the default",         StructrTraits.FILE,   typeAt("/unknownTypes/d/x.txt"));
		assertEquals("A folder type that is not a folder should fall back",          StructrTraits.FOLDER, typeAt("/unknownTypes/d"));
	}

	@Test
	public void testDeletingAnExternalNodeNeverDeletesItsContentDirectly() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("guardExternal").entries(ExternalEntry.file("x.txt", 10L, MODIFIED));

		mountAndAwaitAttach("guardExternal", scenario);

		final NodeInterface x = getNodeAtPath("/guardExternal/x.txt");

		assertOutbound(scenario, () -> app.delete(x));
		assertEquals("The content of an external node belongs to the storage, not to the node", List.of(), scenario.deletes());
	}

	@Test
	public void testDeletingAStructrFileInAnInboundOnlyMountDeletesItsContentDirectly() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("guardInbound");

		mountAndAwaitAttach("guardInbound", scenario);

		final NodeInterface local = createFile("local.txt", getNodeAtPath("/guardInbound"));

		assertOutbound(scenario, () -> app.delete(local));
		assertEquals("Without outbound sync, a file created in Structr is deleted through its provider", List.of("local.txt"), scenario.deletes());
	}

	@Test
	public void testDeletingAStructrFileInATwoWayMountGoesThroughOutboundSync() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("guardBoth");

		mountAndAwaitAttach("guardBoth", scenario, BOTH);

		final NodeInterface local = createFile("local.txt", getNodeAtPath("/guardBoth"));

		assertOutbound(scenario, () -> app.delete(local), "DELETED local.txt");
		assertEquals("With outbound sync, the content is deleted by the synchronizer and not directly", List.of(), scenario.deletes());
	}

	/**
	 * Two scans of one mount would both create the entries the other has not committed yet. A scan
	 * requested while one is running must wait for it, and requests that pile up meanwhile are one scan.
	 */
	@Test
	public void testScansOfOneMountNeverOverlap() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("overlap").entries(ExternalEntry.file("a.txt", 10L, MODIFIED));

		mountAndAwaitAttach("overlap", scenario);

		final int enumerationsBefore = scenario.enumerations();
		final CountDownLatch gate    = scenario.holdNextEnumeration();

		try {

			scenario.requestFullScan();

			waitFor("The first scan should have started", () -> scenario.enumerations() == enumerationsBefore + 1);

			scenario.requestFullScan();
			scenario.requestFullScan();

			// a handled event proves the run loop has been round since the requests
			final long handled = handledEvents();

			scenario.emit(ExternalChangeEvent.modified(ExternalEntry.file("a.txt", 11L, MODIFIED + 1)));
			awaitHandledEventsBeyond(handled);

			assertEquals("No second scan may start while the first is running", enumerationsBefore + 1, scenario.enumerations());

		} finally {

			gate.countDown();
		}

		waitFor("The pending request should be scanned once the first scan has ended", () -> scenario.enumerations() == enumerationsBefore + 2);
		awaitIdle();

		final long handled = handledEvents();

		scenario.emit(ExternalChangeEvent.modified(ExternalEntry.file("a.txt", 12L, MODIFIED + 2)));
		awaitHandledEventsBeyond(handled);

		assertEquals("Requests made during one scan should be one scan", enumerationsBefore + 2, scenario.enumerations());
	}

	@Test
	public void testMovingAnExternalNodeWithoutOutboundSyncIsRefused() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("moveGuard").entries(
			ExternalEntry.directory("a", MODIFIED),
			ExternalEntry.file("x.txt", 10L, MODIFIED)
		);

		mountAndAwaitAttach("moveGuard", scenario);

		final NodeInterface root  = getNodeAtPath("/moveGuard");
		final NodeInterface a     = getNodeAtPath("/moveGuard/a");
		final NodeInterface x     = getNodeAtPath("/moveGuard/x.txt");
		final NodeInterface local = createFile("local.txt", root);

		assertRefused("Moving an external node by parent should be refused",   () -> setParent(x, a));
		assertRefused("Moving an external node by parentId should be refused", () -> x.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PARENT_ID_PROPERTY), a.getUuid()));
		assertNotNull("A refused move should leave the node where it was", getNodeAtPath("/moveGuard/x.txt"));

		// setting the parent it already has is no move, and a node created in Structr may move freely
		assertOutbound(scenario, () -> setParent(x, root));
		assertOutbound(scenario, () -> setParent(local, a));
		assertNotNull("A node created in Structr should move", getNodeAtPath("/moveGuard/a/local.txt"));
	}

	private void assertRefused(final String message, final TxBody change) {

		try (final Tx tx = app.tx()) {

			change.run();

			tx.success();

			fail(message);

		} catch (FrameworkException expected) {

			assertEquals("A refused move should be a client error", 422, expected.getStatus());
			assertTrue("A refused move should name the cause", expected.getMessage().contains("sync direction"));
		}
	}

	/**
	 * Marking a node is a modification and is validated when its batch commits, so an external node
	 * whose name validation rejects, as older data may hold, makes the batch fail.
	 */
	@Test
	public void testFailedReconciliationDoesNotCountAsAScan() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("reconcileFails").entries(ExternalEntry.file("ok.txt", 10L, MODIFIED));

		mountAndAwaitAttach("reconcileFails", scenario, Map.of(VanishPolicy.POLICY_KEY, VanishPolicy.READONLY.value()));

		try (final Tx tx = app.tx(false, false, false)) {

			app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "in\0valid.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/reconcileFails")),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.IS_EXTERNAL_PROPERTY), true)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		final Long scannedBefore     = getLastScanned("reconcileFails");
		final int enumerationsBefore = scenario.enumerations();

		scenario.requestFullScan();

		waitFor("The scan should have started", () -> scenario.enumerations() > enumerationsBefore);
		awaitIdle();

		assertFalse("The node whose batch failed should not be marked", isExternallyDeleted("in\0valid.txt"));
		assertEquals("A scan whose reconciliation failed must not count as complete", scannedBefore, getLastScanned("reconcileFails"));
	}

	@Test
	public void testScanStopsWhenItsRootIsDeleted() {

		final List<ExternalEntry> entries = new ArrayList<>();

		for (int i = 0; i < 1500; i++) {

			entries.add(ExternalEntry.file("file" + i + ".txt", 10L, MODIFIED));
		}

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("rootGone");

		mountAndAwaitAttach("rootGone", scenario);

		final int enumerationsBefore = scenario.enumerations();
		final CountDownLatch gate    = scenario.entries(entries).holdNextEnumeration();
		final NodeInterface root     = getNodeAtPath("/rootGone");

		try {

			scenario.requestFullScan();

			waitFor("The scan should have started", () -> scenario.enumerations() > enumerationsBefore);

			try (final Tx tx = app.tx()) {

				app.delete(root);

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception.");
			}

		} finally {

			gate.countDown();
		}

		awaitIdle();

		assertEquals("A scan whose root is gone should stop instead of going through every entry", 0, scenario.entriesHandedOut());
	}

	/**
	 * An entry whose size and modification time are what the node already records has not changed, so a
	 * scan only marks it as seen: it neither reads the content's metadata again nor rewrites it.
	 */
	@Test
	public void testRescanLeavesUnchangedEntriesAlone() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("unchanged").entries(
			ExternalEntry.file("a.txt", 10L, MODIFIED),
			ExternalEntry.directory("d", MODIFIED),
			ExternalEntry.file("d/b.txt", 20L, MODIFIED)
		);

		mountAndAwaitAttach("unchanged", scenario);

		assertEquals("The node should record the storage's modification time", Long.valueOf(MODIFIED), fileModificationDateAt("/unchanged/a.txt"));

		final int readsBefore = scenario.sizeReads();
		final long before     = System.currentTimeMillis();

		scenario.requestFullScan();

		awaitScanAfter("unchanged", before);

		assertEquals("A rescan should not read the metadata of entries that did not change", readsBefore, scenario.sizeReads());
	}

	/**
	 * Stopping the service must interrupt a scan that is held mid-way rather than wait out its bound, and
	 * nothing the scan had not written by then may be written afterwards.
	 */
	@Test
	public void testStoppingTheServiceEndsAScanInFlightWithoutWriting() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("stopInFlight").entries(ExternalEntry.file("a.txt", 10L, MODIFIED));
		final Services services                         = Services.getInstance();

		mountAndAwaitAttach("stopInFlight", scenario);

		final Long scannedBefore     = getLastScanned("stopInFlight");
		final int enumerationsBefore = scenario.enumerations();
		final CountDownLatch gate    = scenario.entries(ExternalEntry.file("a.txt", 10L, MODIFIED), ExternalEntry.file("b.txt", 10L, MODIFIED)).holdNextEnumeration();

		scenario.requestFullScan();

		waitFor("The scan should have started", () -> scenario.enumerations() > enumerationsBefore);

		try {

			final long start = System.currentTimeMillis();

			services.shutdownService("StorageSyncService.default");

			assertTrue("Stopping should interrupt the held scan instead of waiting for it", System.currentTimeMillis() - start < 5_000);

			gate.countDown();

			assertNull("A scan stopped before it got to an entry must not write it", getNodeAtPath("/stopInFlight/b.txt"));
			assertEquals("A stopped scan must not advance the scan time", scannedBefore, getLastScanned("stopInFlight"));

		} finally {

			gate.countDown();

			try {

				services.startService("StorageSyncService.default");

			} catch (Exception ex) {

				ex.printStackTrace();
				fail("Unexpected exception while restarting the service: " + ex.getMessage());
			}
		}
	}

	/**
	 * Runs the given changes in one transaction and asserts the outbound events the scenario's mount
	 * receives for them, in order, as "TYPE path" or "MOVED from -> to".
	 */
	private void assertOutbound(final ScriptedStorageProvider.Scenario scenario, final TxBody changes, final String... expected) {

		scenario.outbound().clear();

		try (final Tx tx = app.tx()) {

			changes.run();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		awaitIdle();

		assertEquals("Unexpected outbound events", List.of(expected), describeOutbound(scenario));
	}

	private static List<String> describeOutbound(final ScriptedStorageProvider.Scenario scenario) {
		return scenario.outbound().stream().map(StorageSyncScenarioTest::describe).toList();
	}

	private static String describe(final VirtualChangeEvent event) {

		return switch (event.type()) {
			case CREATED -> "CREATED " + event.relativePath();
			case MOVED   -> "MOVED " + event.previousRelativePath() + " -> " + event.relativePath();
			case DELETED -> "DELETED " + event.previousRelativePath();
		};
	}

	/**
	 * Deletes a folder the way the Files UI does: every node Folder.getAllChildNodes() lists, in that
	 * order, then the folder itself, all in the caller's transaction.
	 */
	private void deleteRecursively(final NodeInterface folder) throws FrameworkException {

		for (final NodeInterface child : folder.as(Folder.class).getAllChildNodes()) {

			app.delete(child);
		}

		app.delete(folder);
	}

	private String uuidAt(final String path) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = getNodeAtPath(path);

			tx.success();

			return node != null ? node.getUuid() : null;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private Long fileModificationDateAt(final String path) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = getNodeAtPath(path);

			tx.success();

			return node != null ? node.as(File.class).getFileModificationDate() : null;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private String typeAt(final String path) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = getNodeAtPath(path);

			tx.success();

			return node != null ? node.getType() : null;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private NodeInterface createFile(final String name, final NodeInterface parent) {

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), parent)
			);

			tx.success();

			return file;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");

			return null;
		}
	}

	private NodeInterface createFolder(final String name, final NodeInterface parent) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.FOLDER);

		return app.create(StructrTraits.FOLDER,
			new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
			new NodeAttribute<>(traits.key(AbstractFileTraitDefinition.PARENT_PROPERTY), parent)
		);
	}

	private void setName(final NodeInterface node, final String name) throws FrameworkException {
		node.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);
	}

	private void setParent(final NodeInterface node, final NodeInterface parent) throws FrameworkException {
		node.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), parent);
	}

	@FunctionalInterface
	private interface TxBody {
		void run() throws FrameworkException;
	}

	/**
	 * Mounts a watched folder on the given scenario and waits until it is attached and its initial scan
	 * is complete.
	 *
	 * @return the uuid of the mount root
	 */
	private String mountAndAwaitAttach(final String name, final ScriptedStorageProvider.Scenario scenario) {
		return mountAndAwaitAttach(name, scenario, Map.of());
	}

	private String mountAndAwaitAttach(final String name, final ScriptedStorageProvider.Scenario scenario, final Map<String, String> extraEntries) {

		mountScripted(name, scenario, true, null, extraEntries);

		waitFor("The mount should be attached", () -> scenario.synchronizersCreated() >= 1);
		awaitScanAfter(name, 0L);

		try (final Tx tx = app.tx()) {

			final String uuid = app.nodeQuery(StructrTraits.FOLDER).name(name).getFirst().getUuid();

			tx.success();

			return uuid;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private StorageSyncService service() {
		return StructrApp.getInstance().getService(StorageSyncService.class);
	}

	private NodeInterface getNode(final String path) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.ABSTRACT_FILE).key(Traits.of(StructrTraits.ABSTRACT_FILE).key("path"), path).getFirst();

			tx.success();

			return node;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private Long getLastScanned(final String folderName) {

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).name(folderName).getFirst();
			final Long lastScanned     = folder.getProperty(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_LAST_SCANNED_PROPERTY));

			tx.success();

			return lastScanned;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}
}
