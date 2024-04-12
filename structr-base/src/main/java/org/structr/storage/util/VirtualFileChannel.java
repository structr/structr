/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.storage.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.*;

public class VirtualFileChannel extends FileChannel {
    private final SeekableByteChannel internalChannel;

    public VirtualFileChannel(final SeekableByteChannel internalChannel) {
        this.internalChannel = internalChannel;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return internalChannel.read(dst);
    }

    @Override
    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        long bytesRead = 0;
        internalChannel.position(offset);
        internalChannel.truncate(length);

        for (ByteBuffer buff : dsts) {
            bytesRead += internalChannel.read(buff);
        }

        return bytesRead;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        return internalChannel.write(src);
    }

    @Override
    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        // ToDo: Validate correct behaviour
        long bytesWritten = 0;
        internalChannel.position(offset);
        internalChannel.truncate(length);

        for (ByteBuffer buff : srcs) {
            bytesWritten += internalChannel.write(buff);
        }

        return bytesWritten;
    }

    @Override
    public long position() throws IOException {
        return internalChannel.position();
    }

    @Override
    public FileChannel position(long newPosition) throws IOException {
        internalChannel.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return internalChannel.size();
    }

    @Override
    public FileChannel truncate(long size) throws IOException {
        internalChannel.truncate(size);
        return this;
    }

    @Override
    public void force(boolean metaData) throws IOException {
        // can't force seekablebytechannel
        //throw new IOException("Operation not supported");
    }

    @Override
    public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
        throw new IOException("Operation not supported");
    }

    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        throw new IOException("Operation not supported");
    }

    @Override
    public int read(ByteBuffer dst, long position) throws IOException {
        internalChannel.position(position);
        return read(dst);
    }

    @Override
    public int write(ByteBuffer src, long position) throws IOException {
        internalChannel.position(position);
        return write(src);
    }

    @Override
    public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
        throw new IOException("Operation not supported");
    }

    @Override
    public FileLock lock(long position, long size, boolean shared) throws IOException {
        throw new IOException("Operation not supported");
    }

    @Override
    public FileLock tryLock(long position, long size, boolean shared) throws IOException {
        throw new IOException("Operation not supported");
    }

    @Override
    protected void implCloseChannel() throws IOException {
        internalChannel.close();
    }
}
