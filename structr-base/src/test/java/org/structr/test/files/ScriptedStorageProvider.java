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

import org.structr.storage.AbstractStorageProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.sync.ExternalChangeEvent;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.StorageSyncListener;
import org.structr.storage.sync.StorageSynchronizer;
import org.structr.storage.sync.SyncTarget;
import org.structr.storage.sync.SynchronizableStorageProvider;
import org.structr.storage.sync.VirtualChangeEvent;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.StorageConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only storage provider whose synchronizer does what a test scripts: the entries each scan
 * enumerates and their order, failures at a chosen point, a gate that holds a scan mid-enumeration,
 * and the events of a watch. It counts what the service asks of it, so a test can wait for a scan or
 * an attach instead of sleeping. A storage configuration picks its scenario with the entry
 * {@value #SCENARIO_KEY}.
 */
public class ScriptedStorageProvider extends AbstractStorageProvider implements SynchronizableStorageProvider {

	public static final String SCENARIO_KEY = "fake.scenario";

	private static final Map<String, Scenario> SCENARIOS = new ConcurrentHashMap<>();

	public ScriptedStorageProvider(final AbstractFile file, final StorageConfiguration config) {
		super(file, config);
	}

	public static Scenario scenario(final String name) {
		return SCENARIOS.computeIfAbsent(name, Scenario::new);
	}

	public static void reset() {
		SCENARIOS.clear();
	}

	@Override
	public StorageSynchronizer createSynchronizer(final SyncTarget target) throws IOException {

		final Scenario scenario = SCENARIOS.get(target.configuration().get(SCENARIO_KEY));
		if (scenario == null || scenario.createFails) {

			throw new IOException("No scripted synchronizer for " + target.syncRootPath());
		}

		if (scenario.noSynchronizer) {

			return null;
		}

		scenario.synchronizersCreated.incrementAndGet();

		return new StorageSynchronizer() {

			@Override
			public SyncTarget getTarget() {
				return target;
			}

			@Override
			public boolean supportsWatching() {
				return scenario.supportsWatching;
			}

			@Override
			public boolean tracksEntryPaths() {
				return scenario.tracksEntryPaths;
			}

			@Override
			public void startWatching(final StorageSyncListener listener) {

				scenario.watchStarts.incrementAndGet();
				scenario.listener = listener;
			}

			@Override
			public Iterator<ExternalEntry> enumerate(final String relativePath) throws IOException {

				scenario.enumerations.incrementAndGet();

				if (scenario.failImmediately) {

					throw new IOException("Scripted failure before the first entry");
				}

				final List<ExternalEntry> entries = scenario.entries;
				final Iterator<ExternalEntry> it  = entries.iterator();

				return new Iterator<>() {

					private int handedOut       = 0;
					private boolean gatePassed  = false;

					@Override
					public boolean hasNext() {

						if (!gatePassed) {

							gatePassed = true;
							scenario.awaitGate();
						}

						if (scenario.failAfter >= 0 && handedOut >= scenario.failAfter) {

							throw new UncheckedIOException(new IOException("Scripted failure after " + handedOut + " entries"));
						}

						return it.hasNext();
					}

					@Override
					public ExternalEntry next() {

						handedOut++;
						scenario.entriesHandedOut.incrementAndGet();

						return it.next();
					}
				};
			}

			@Override
			public void onVirtualChange(final VirtualChangeEvent event) {
				scenario.outbound.add(event);
			}

			@Override
			public void close() {
				scenario.closes.incrementAndGet();
			}
		};
	}

	// ----- content, consistent with the scripted sizes so that an unchanged entry reads as unchanged -----
	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(new byte[(int) Math.max(0L, size())]);
	}

	@Override
	public OutputStream getOutputStream() {
		return new ByteArrayOutputStream();
	}

	@Override
	public OutputStream getOutputStream(final boolean append) {
		return new ByteArrayOutputStream();
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public String getName() {
		return getAbstractFile() != null ? getAbstractFile().getName() : null;
	}

	@Override
	public SeekableByteChannel getSeekableByteChannel(final Set<? extends OpenOption> options) {
		return null;
	}

	@Override
	public void delete() {

		final Scenario scenario = ownScenario();
		if (scenario != null) {

			scenario.deletes.add(getName());
		}
	}

	@Override
	public long size() {

		final Scenario scenario = ownScenario();
		if (scenario != null) {

			scenario.sizeReads.incrementAndGet();
		}

		final ExternalEntry entry = scriptedEntry();

		return entry != null && entry.size() != null ? entry.size() : 0L;
	}

	private ExternalEntry scriptedEntry() {

		final AbstractFile file = getAbstractFile();
		final Scenario scenario = ownScenario();

		if (file == null || scenario == null) {

			return null;
		}

		final AbstractFile root = StorageProviderFactory.getStorageConfigurationSupplier(file);
		final String rootPath   = root != null ? root.getPath() : null;
		final String path       = file.getPath();

		if (rootPath == null || path == null || !path.startsWith(rootPath + "/")) {

			return null;
		}

		final String relativePath = path.substring(rootPath.length() + 1);

		for (final ExternalEntry entry : scenario.entries) {

			if (relativePath.equals(entry.relativePath())) {

				return entry;
			}
		}

		return null;
	}

	private Scenario ownScenario() {

		final StorageConfiguration sc = getConfig();

		return sc != null ? SCENARIOS.get(sc.getConfiguration().get(SCENARIO_KEY)) : null;
	}

	/**
	 * What one storage configuration's synchronizer does. Every setting may be changed while the mount
	 * is attached and takes effect from the next enumeration.
	 */
	public static final class Scenario {

		private final String name;

		private volatile List<ExternalEntry> entries = List.of();
		private volatile int failAfter               = -1;
		private volatile boolean failImmediately     = false;
		private volatile boolean createFails         = false;
		private volatile boolean noSynchronizer      = false;
		private volatile boolean supportsWatching    = true;
		private volatile boolean tracksEntryPaths    = false;
		private volatile CountDownLatch gate         = null;
		private volatile StorageSyncListener listener = null;

		private final AtomicInteger synchronizersCreated = new AtomicInteger();
		private final AtomicInteger enumerations         = new AtomicInteger();
		private final AtomicInteger entriesHandedOut     = new AtomicInteger();
		private final AtomicInteger sizeReads            = new AtomicInteger();
		private final AtomicInteger watchStarts          = new AtomicInteger();
		private final AtomicInteger closes               = new AtomicInteger();
		private final List<VirtualChangeEvent> outbound  = new CopyOnWriteArrayList<>();
		private final List<String> deletes               = new CopyOnWriteArrayList<>();

		private Scenario(final String name) {
			this.name = name;
		}

		public String name()                            { return name; }
		public Scenario entries(final ExternalEntry... e) { this.entries = List.of(e); return this; }
		public Scenario entries(final List<ExternalEntry> e) { this.entries = List.copyOf(e); return this; }
		public Scenario failAfter(final int n)           { this.failAfter = n; return this; }
		public Scenario failImmediately(final boolean f) { this.failImmediately = f; return this; }
		public Scenario createFails(final boolean f)     { this.createFails = f; return this; }
		public Scenario noSynchronizer(final boolean f)  { this.noSynchronizer = f; return this; }
		public Scenario supportsWatching(final boolean w) { this.supportsWatching = w; return this; }
		public Scenario tracksEntryPaths(final boolean t) { this.tracksEntryPaths = t; return this; }

		/** Holds the next enumeration before its first entry until the returned latch is counted down. */
		public CountDownLatch holdNextEnumeration() {

			final CountDownLatch latch = new CountDownLatch(1);
			this.gate = latch;

			return latch;
		}

		public int synchronizersCreated() { return synchronizersCreated.get(); }
		public int enumerations()         { return enumerations.get(); }
		public int entriesHandedOut()     { return entriesHandedOut.get(); }
		public int sizeReads()            { return sizeReads.get(); }
		public int watchStarts()          { return watchStarts.get(); }
		public int closes()               { return closes.get(); }
		public List<VirtualChangeEvent> outbound() { return outbound; }

		/** The names of the files whose content was deleted directly through the provider. */
		public List<String> deletes() { return deletes; }

		/** Delivers an event as the watch of the attached synchronizer would. */
		public void emit(final ExternalChangeEvent event) {

			if (listener == null) {

				throw new IllegalStateException("Scenario " + name + " is not being watched");
			}

			listener.onEvent(event);
		}

		public void requestFullScan() {

			if (listener == null) {

				throw new IllegalStateException("Scenario " + name + " is not being watched");
			}

			listener.requestFullScan();
		}

		private void awaitGate() {

			final CountDownLatch latch = gate;
			if (latch != null) {

				gate = null;

				// bounded, so a test that forgets to release the gate fails instead of hanging the fork
				try { latch.await(60, TimeUnit.SECONDS); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
			}
		}
	}
}
