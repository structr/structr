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
package org.structr.storage.sync;

/**
 * A single change observed in an external storage backend. The affected
 * object is identified by the entry's nodeUuid and/or relativePath (see
 * {@link ExternalEntry}).
 *
 * previousRelativePath is only set for path-addressed MOVED events.
 * UUID-addressed backends express a move/rename as MODIFIED with a new
 * relativePath (the UUID is stable), or simply as MODIFIED - the node's
 * own name and parent remain authoritative in that case.
 */
public record ExternalChangeEvent(Type type, ExternalEntry entry, String previousRelativePath) {

	public enum Type {

		/** entry appeared in the external storage */
		CREATED,

		/** entry content or attributes changed */
		MODIFIED,

		/** entry vanished (the entry carries addressing information only) */
		DELETED,

		/** entry moved/renamed within the same synchronized subtree */
		MOVED
	}

	public ExternalChangeEvent {

		if (type == null || entry == null) {
			throw new IllegalArgumentException("ExternalChangeEvent needs a type and an entry");
		}

		if (Type.MOVED.equals(type) && previousRelativePath == null) {
			throw new IllegalArgumentException("MOVED events need a previousRelativePath");
		}
	}

	public static ExternalChangeEvent created(final ExternalEntry entry) {
		return new ExternalChangeEvent(Type.CREATED, entry, null);
	}

	public static ExternalChangeEvent modified(final ExternalEntry entry) {
		return new ExternalChangeEvent(Type.MODIFIED, entry, null);
	}

	public static ExternalChangeEvent deleted(final ExternalEntry entryReference) {
		return new ExternalChangeEvent(Type.DELETED, entryReference, null);
	}

	public static ExternalChangeEvent moved(final String fromRelativePath, final ExternalEntry to) {
		return new ExternalChangeEvent(Type.MOVED, to, fromRelativePath);
	}
}
