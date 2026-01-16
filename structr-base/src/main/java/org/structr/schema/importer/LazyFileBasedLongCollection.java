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
package org.structr.schema.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.graph.SyncCommand;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 *
 */
public class LazyFileBasedLongCollection implements Collection<Long>, Closeable {

	private static final Logger logger = LoggerFactory.getLogger(LazyFileBasedLongCollection.class.getName());

	private DataOutputStream dos = null;
	private File file            = null;

	public LazyFileBasedLongCollection(final String path) {
		this(path, true);
	}

	public LazyFileBasedLongCollection(final String path, final boolean clearOnOpen) {

		this.file = new File(path);

		file.getParentFile().mkdirs();

		// remove file if it exists
		if (clearOnOpen && file.exists()) {
			file.delete();
		}

		open(true);
	}

	@Override
	public int hashCode() {
		return file.hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof LazyFileBasedLongCollection && o.hashCode() == this.hashCode();
	}

	@Override
	public int size() {

		int count = 0;

		if (file.exists()) {

			for (Iterator<Long> it = iterator(); it.hasNext(); it.next()) {
				count++;
			}
		}

		return count;
	}

	@Override
	public boolean isEmpty() {
		return !iterator().hasNext();
	}

	@Override
	public boolean contains(Object o) {

		for (final Long val : this) {

			if (val.equals(o)) {

				return true;
			}
		}

		return false;
	}

	@Override
	public Iterator<Long> iterator() {
		return new FileIterator(file);
	}

	@Override
	public Object[] toArray() {

		final ArrayList<Long> list = new ArrayList<>();
		for (final Long val : this) {
			list.add(val);
		}

		return list.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {

		final ArrayList<Long> list = new ArrayList<>();
		for (final Long val : this) {
			list.add(val);
		}

		return list.toArray(a);
	}

	@Override
	public boolean add(Long e) {

		try {

			SyncCommand.serialize(dos, e);

		} catch (IOException ex) {
			logger.error("", ex);
		}

		return true;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean addAll(Collection<? extends Long> toAdd) {

		if (toAdd.hashCode() == this.hashCode()) {
			return true;
		}

		try{

			for (final Long t : toAdd) {
				SyncCommand.serialize(dos, t);
			}

		} catch (IOException ex) {
			logger.error("", ex);
		}

		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void clear() {
		open(false);
	}

	@Override
	public void close() throws IOException {

		dos.flush();
		dos.close();
	}

	private void open(final boolean append) {

		try {

			if (dos != null) {
				dos.close();
			}

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		try {

			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, append)));

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	private static class FileIterator implements Iterator<Long> {

		private DataInputStream dis = null;
		private Long currentObject  = null;

		public FileIterator(final File file) {

			try {

				dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

			} catch (IOException ioex) {

				logger.warn("", ioex);
			}
		}

		@Override
		public boolean hasNext() {

			try {
				currentObject = (Long)SyncCommand.deserialize(dis);
				if (currentObject != null) {

					return true;
				}

			} catch (IOException ex) { }

			close();

			return false;
		}

		@Override
		public Long next() {
			return currentObject;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not supported.");
		}

		// ----- private methods -----
		private void close() {

			try {

				dis.close();

			} catch (IOException ioex) {
				logger.warn("", ioex);
			}
		}

	}
}
