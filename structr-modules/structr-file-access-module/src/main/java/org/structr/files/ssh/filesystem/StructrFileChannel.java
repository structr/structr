/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.files.ssh.filesystem;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 *
 */
public class StructrFileChannel extends FileChannel {

	private FileOutputStream fos = null;

	public StructrFileChannel(final FileOutputStream fos) {
		this.fos   = fos;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		return fos.getChannel().read(dst);
	}

	@Override
	public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
		return fos.getChannel().read(dsts, offset, length);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return fos.getChannel().write(src);
	}

	@Override
	public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
		return fos.getChannel().write(srcs, offset, length);
	}

	@Override
	public long position() throws IOException {
		return fos.getChannel().position();
	}

	@Override
	public FileChannel position(long newPosition) throws IOException {
		return fos.getChannel().position(newPosition);
	}

	@Override
	public long size() throws IOException {
		return fos.getChannel().size();
	}

	@Override
	public FileChannel truncate(long size) throws IOException {
		return fos.getChannel().truncate(size);
	}

	@Override
	public void force(boolean metaData) throws IOException {
		fos.getChannel().force(metaData);
	}

	@Override
	public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
		return fos.getChannel().transferTo(position, count, target);
	}

	@Override
	public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
		return fos.getChannel().transferFrom(src, position, count);
	}

	@Override
	public int read(ByteBuffer dst, long position) throws IOException {
		return fos.getChannel().read(dst, position);
	}

	@Override
	public int write(ByteBuffer src, long position) throws IOException {
		return fos.getChannel().write(src, position);
	}

	@Override
	public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
		return fos.getChannel().map(mode, position, size);
	}

	@Override
	public FileLock lock(long position, long size, boolean shared) throws IOException {
		return fos.getChannel().lock(position, size, shared);
	}

	@Override
	public FileLock tryLock(long position, long size, boolean shared) throws IOException {
		return fos.getChannel().tryLock(position, size, shared);
	}

	@Override
	protected void implCloseChannel() throws IOException {
		fos.close();
	}
}
