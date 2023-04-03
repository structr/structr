/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.core.storage.memory;

import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.slf4j.LoggerFactory;
import org.structr.core.app.StructrApp;
import org.structr.core.storage.AbstractStorageProvider;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;

import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStorageProvider extends AbstractStorageProvider {
	private static final Map<String, byte[]> dataMap = new ConcurrentHashMap<>();

	public InMemoryStorageProvider(final AbstractFile file) {
		super(file);
	}

	@Override
	public InputStream getInputStream() {

		if (dataMap.get(getFile().getUuid()) != null) {
			return new ByteArrayInputStream(dataMap.get(getFile().getUuid()));
		}

		return new ByteArrayInputStream(new byte[]{});
	}

	@Override
	public OutputStream getOutputStream() {
		return new InMemoryOutputStream(false);
	}

	@Override
	public String getContentType() {
		return getFile().getProperty(StructrApp.key(File.class, "contentType"));
	}

	@Override
	public String getName() {
		return getFile().getName();
	}

	@Override
	public OutputStream getOutputStream(boolean append) {
		return new InMemoryOutputStream(append);
	}

	@Override
	public SeekableByteChannel getSeekableByteChannel() {

		return new SavingInMemorySeekableByteChannel();
	}

	@Override
	public void delete() {
		dataMap.remove(getFile().getUuid());
	}

	@Override
	public long size() {
		return dataMap.get(getFile().getUuid()) != null ? dataMap.get(getFile().getUuid()).length : 0;
	}

	private class SavingInMemorySeekableByteChannel extends SeekableInMemoryByteChannel {
		public SavingInMemorySeekableByteChannel() {
			super(0);
		}
		public SavingInMemorySeekableByteChannel(final byte[] data) {
			super(data);
		}
		@Override
		public void close() {

			dataMap.put(getFile().getUuid(), Arrays.copyOf(super.array(), (int) super.size()));
		}
	}
	private class InMemoryOutputStream extends ByteArrayOutputStream {
		private final boolean append;

		public InMemoryOutputStream(final boolean append) {
			super();
			this.append = append;
		}

		@Override
		public void flush() {
			saveBuffer();
		}

		@Override
		public void close() {
			saveBuffer();
		}

		private void saveBuffer() {
			final String uuid = getFile().getUuid();

			if (append) {

				try {
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					bos.write(dataMap.get(uuid));
					bos.write(super.buf);
					dataMap.put(uuid, bos.toByteArray());
				} catch (IOException ex) {

					LoggerFactory.getLogger(InMemoryStorageProvider.class).error("Could not append byte[] for file data.", ex);
				}
			} else {

				dataMap.put(uuid, super.buf);
			}
		}
	}
}
