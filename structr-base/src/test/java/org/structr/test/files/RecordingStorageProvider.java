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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test-only storage provider that stores nothing and records all outbound
 * VirtualChangeEvents in a static list - stands in for backends whose
 * physical keys do not depend on the virtual path and that may simply
 * ignore structural changes.
 */
public class RecordingStorageProvider extends AbstractStorageProvider implements SynchronizableStorageProvider {

	public static final List<VirtualChangeEvent> RECORDED_EVENTS = new CopyOnWriteArrayList<>();

	public RecordingStorageProvider(final AbstractFile file, final StorageConfiguration config) {
		super(file, config);
	}

	public static void reset() {
		RECORDED_EVENTS.clear();
	}

	@Override
	public StorageSynchronizer createSynchronizer(final SyncTarget target) {

		return new StorageSynchronizer() {

			@Override
			public SyncTarget getTarget() {
				return target;
			}

			@Override
			public boolean supportsWatching() {
				return false;
			}

			@Override
			public void startWatching(final StorageSyncListener listener) {
			}

			@Override
			public Iterator<ExternalEntry> enumerate(final String relativePath) {
				return Collections.emptyIterator();
			}

			@Override
			public void onVirtualChange(final VirtualChangeEvent event) {
				RECORDED_EVENTS.add(event);
			}

			@Override
			public void close() {
			}
		};
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(new byte[0]);
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
	}

	@Override
	public long size() {
		return 0;
	}
}
