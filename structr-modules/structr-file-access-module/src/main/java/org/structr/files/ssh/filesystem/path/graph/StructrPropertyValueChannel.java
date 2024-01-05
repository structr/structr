/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.files.ssh.filesystem.path.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.PropertyKey;
import org.structr.files.ssh.filesystem.StructrPath;
import org.structr.files.ssh.filesystem.StructrPath.HiddenFileEntry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;

/**
 *
 */
public class StructrPropertyValueChannel<T extends GraphObject> extends FileChannel {

	private static final Logger logger = LoggerFactory.getLogger(StructrPropertyValueChannel.class.getName());

	private ByteBuffer buffer               = ByteBuffer.allocate(65535);
	private SecurityContext securityContext = null;
	private GraphObject graphObject         = null;
	private PropertyKey key                 = null;
	private boolean dataWasWritten          = false;
	private boolean truncate                = false;
	private boolean append                  = false;

	public StructrPropertyValueChannel(final SecurityContext securityContext, final GraphObject src, final PropertyKey key, final boolean truncate, final boolean append) {

		this.securityContext = securityContext;
		this.truncate        = truncate;
		this.append          = append;
		this.graphObject     = src;
		this.key             = key;
	}

	@Override
	public int read(final ByteBuffer dst) throws IOException {

		try {
			final String stringValue = getConvertedPropertyValue(securityContext, graphObject, key);
			final byte[] bytes       = stringValue.getBytes(Charset.forName("utf-8"));

			dst.put(bytes);

			return bytes.length;

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		return 0;
	}

	@Override
	public long read(final ByteBuffer[] dsts, final int offset, final int length) throws IOException {
		return 0L;
	}

	@Override
	public int write(final ByteBuffer src) throws IOException {

		checkWriteAccess(key);

		final int len = src.remaining();
		buffer.put(src);

		dataWasWritten = true;

		return len;
	}

	@Override
	public long write(final ByteBuffer[] srcs, int offset, int length) throws IOException {

		checkWriteAccess(key);

		dataWasWritten = true;
		return 0L;
	}

	@Override
	public long position() throws IOException {
		return 0L;
	}

	@Override
	public FileChannel position(final long newPosition) throws IOException {

		checkWriteAccess(key);

		return this;
	}

	@Override
	public long size() throws IOException {

		try {

			return getConvertedPropertyValue(securityContext, graphObject, key).getBytes(Charset.forName("utf-8")).length;

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		return 0L;
	}

	@Override
	public FileChannel truncate(final long size) throws IOException {

		checkWriteAccess(key);

		return this;
	}

	@Override
	public void force(final boolean metaData) throws IOException {
	}

	@Override
	public long transferTo(final long position, final long count, final WritableByteChannel target) throws IOException {
		return 0L;
	}

	@Override
	public long transferFrom(final ReadableByteChannel src, final long position, final long count) throws IOException {

		checkWriteAccess(key);

		return 0L;
	}

	@Override
	public int read(final ByteBuffer dst, final long position) throws IOException {
		return 0;
	}

	@Override
	public int write(final ByteBuffer src, final long position) throws IOException {

		checkWriteAccess(key);

		return 0;
	}

	@Override
	public MappedByteBuffer map(final MapMode mode, final long position, long size) throws IOException {
		return null;
	}

	@Override
	public FileLock lock(final long position, long size, final boolean shared) throws IOException {

		checkWriteAccess(key);

		return null;
	}

	@Override
	public FileLock tryLock(final long position, final long size, final boolean shared) throws IOException {

		checkWriteAccess(key);

		return null;
	}

	@Override
	protected void implCloseChannel() throws IOException {

		if (dataWasWritten && graphObject != null) {

			try {
				buffer.flip();
				final byte[] data  = new byte[buffer.remaining()];
				buffer.get(data);

				final String value = new String(data, Charset.forName("utf-8"));
				final StringBuilder buf = new StringBuilder();

				if (append) {

					// append previous value to buffer
					buf.append(getConvertedPropertyValue(securityContext, graphObject, key));
				}

				// append new value to buffer
				buf.append(value);

				// store property value
				setConvertedPropertyValue(securityContext, graphObject, key, value);

			} catch (FrameworkException fex) {
				logger.warn("", fex);
			}
		}
	}

	// ----- public static methods -----
	public static String getConvertedPropertyValue(final SecurityContext securityContext, final GraphObject graphObject, final PropertyKey key) throws IOException, FrameworkException {

		final PropertyConverter inputConverter = key.inputConverter(securityContext);
		Object actualValue                     = graphObject.getProperty(key);

		if (inputConverter != null) {
			actualValue = inputConverter.revert(actualValue);
		}

		if (actualValue != null) {
			return actualValue.toString();
		}

		return "";
	}

	public static void setConvertedPropertyValue(final SecurityContext securityContext, final GraphObject graphObject, final PropertyKey key, final String value) throws IOException, FrameworkException {

		final PropertyConverter converter = key.inputConverter(securityContext);
		final String previousUuid         = graphObject.getUuid();
		Object actualValue                = value;

		if (converter != null) {
			actualValue = converter.convert(actualValue);
		}

		if (key.isReadOnly()) {
			graphObject.unlockReadOnlyPropertiesOnce();
		}

		if (key.isSystemInternal()) {
			graphObject.unlockSystemPropertiesOnce();
		}

		// set value
		graphObject.setProperty(key, actualValue);

		// move "hidden" entries to new UUID
		if (GraphObject.id.equals(key)) {

			final HiddenFileEntry entry = StructrPath.HIDDEN_PROPERTY_FILES.get(previousUuid);

			if (entry != null) {

				StructrPath.HIDDEN_PROPERTY_FILES.remove(previousUuid);
				StructrPath.HIDDEN_PROPERTY_FILES.put(value, entry);
			}
		}
	}

	// ----- private methods -----
	private static void handleFrameworkException(final FrameworkException fex, final String name) throws IOException {

		switch (fex.getStatus()) {

			case 401:
			case 403:
				throw new AccessDeniedException(name);

			case 404:
				throw new NoSuchFileException(name);

			default:
				throw new IllegalArgumentException(fex.getMessage());
		}
	}

	public static void checkWriteAccess(final PropertyKey key) throws IOException {

		/*
		if (key.isSystemInternal() || key.isReadOnly() || key.isWriteOnce()) {
			throw new AccessDeniedException(key.jsonName());
		}
		*/
	}
}
