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
 * A structural change of the Structr virtual filesystem (outbound
 * counterpart of {@link ExternalChangeEvent}): a file or folder governed by
 * an outbound-enabled sync target was created, moved/renamed or deleted
 * inside Structr, and the change was committed.
 *
 * Paths are '/'-separated and relative to the sync root, normalized like
 * {@link ExternalEntry} paths. The event is self-contained - synchronizers
 * receive it outside of any transaction and must not access graph nodes.
 *
 * A moved/renamed folder produces ONE event for the folder node itself;
 * events for its (unchanged) children are never emitted. Path-based
 * backends move the whole subtree in one operation, backends whose
 * physical keys do not depend on the virtual path simply ignore
 * structural changes.
 *
 * nativeKey is the node's persisted provider-specific physical key
 * (storageKey) captured at event time, or null for uuid-keyed / Structr-origin
 * nodes; path-based backends ignore it.
 */
public record VirtualChangeEvent(Type type, String nodeUuid, boolean directory, String previousRelativePath, String relativePath, String nativeKey) {

	public enum Type {

		/** entry appeared in the virtual filesystem (folders only; file content materializes through the provider's write path) */
		CREATED,

		/** entry was moved and/or renamed within the same sync target */
		MOVED,

		/** entry was deleted (or moved out of the sync target) */
		DELETED
	}

	public VirtualChangeEvent {

		if (type == null || nodeUuid == null) {
			throw new IllegalArgumentException("VirtualChangeEvent needs a type and a nodeUuid");
		}

		switch (type) {

			case CREATED -> {
				if (relativePath == null || previousRelativePath != null) {
					throw new IllegalArgumentException("CREATED events need a relativePath and no previousRelativePath");
				}
			}

			case MOVED -> {
				if (relativePath == null || previousRelativePath == null) {
					throw new IllegalArgumentException("MOVED events need both previousRelativePath and relativePath");
				}
			}

			case DELETED -> {
				if (previousRelativePath == null || relativePath != null) {
					throw new IllegalArgumentException("DELETED events need a previousRelativePath and no relativePath");
				}
			}
		}
	}

	public static VirtualChangeEvent created(final String nodeUuid, final boolean directory, final String relativePath, final String nativeKey) {
		return new VirtualChangeEvent(Type.CREATED, nodeUuid, directory, null, relativePath, nativeKey);
	}

	public static VirtualChangeEvent moved(final String nodeUuid, final boolean directory, final String previousRelativePath, final String relativePath, final String nativeKey) {
		return new VirtualChangeEvent(Type.MOVED, nodeUuid, directory, previousRelativePath, relativePath, nativeKey);
	}

	public static VirtualChangeEvent deleted(final String nodeUuid, final boolean directory, final String previousRelativePath, final String nativeKey) {
		return new VirtualChangeEvent(Type.DELETED, nodeUuid, directory, previousRelativePath, null, nativeKey);
	}
}
