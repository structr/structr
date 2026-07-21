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
 * Provider-neutral description of one entry in an external storage backend,
 * addressed by Structr virtual path and/or node UUID.
 *
 * relativePath is the '/'-separated Structr virtual path relative to the
 * sync root (the AbstractFile that carries the StorageConfiguration), e.g.
 * "sub/dir/file.txt". How a provider derives it (or a nodeUuid) from its
 * physical keys is the provider's own concern - the only requirement is the
 * round-trip invariant: a sync event must address the same file/folder that
 * the original virtual-to-physical conversion started from.
 *
 * nodeUuid identifies the graph node directly. Backends whose physical keys
 * ARE node UUIDs (e.g. an S3 bucket storing objects under the File node's
 * UUID) use this form and may omit the path entirely.
 *
 * At least one of relativePath / nodeUuid must be non-null. size and
 * lastModified may be null when the backend cannot provide them cheaply; the
 * sync service then refreshes metadata unconditionally on modification
 * events.
 *
 * nativeKey is diagnostic only UNLESS bindNativeKey is true. When bindNativeKey
 * is set, nativeKey is the provider's semantic physical identifier of an
 * externally created object (e.g. an arbitrary S3 object key): the sync handler
 * persists it to the resolved node's storageKey on creation, resolves the entry
 * by it, and refuses to bind the entry to a node that carries a different (or
 * no) storageKey. Structr-origin entries leave bindNativeKey false and keep the
 * uuid/path addressing convention.
 */
public record ExternalEntry(String relativePath, String nodeUuid, boolean directory, Long size, Long lastModified, String nativeKey, boolean bindNativeKey) {

	public ExternalEntry {

		if (relativePath == null && nodeUuid == null) {
			throw new IllegalArgumentException("ExternalEntry needs at least one of relativePath or nodeUuid");
		}

		if (bindNativeKey && nativeKey == null) {
			throw new IllegalArgumentException("ExternalEntry with bindNativeKey needs a nativeKey");
		}

		relativePath = normalize(relativePath);
	}

	public static ExternalEntry file(final String relativePath, final Long size, final Long lastModified) {
		return new ExternalEntry(relativePath, null, false, size, lastModified, null, false);
	}

	public static ExternalEntry directory(final String relativePath, final Long lastModified) {
		return new ExternalEntry(relativePath, null, true, null, lastModified, null, false);
	}

	public static ExternalEntry byUuid(final String nodeUuid, final boolean directory, final Long size, final Long lastModified) {
		return new ExternalEntry(null, nodeUuid, directory, size, lastModified, null, false);
	}

	public static ExternalEntry byUuidAndPath(final String nodeUuid, final String relativePath, final boolean directory, final Long size, final Long lastModified) {
		return new ExternalEntry(relativePath, nodeUuid, directory, size, lastModified, null, false);
	}

	/**
	 * A file whose provider key is the given native key (not a Structr node
	 * uuid) - an object created in the backend by an external source. The
	 * relativePath places it in the virtual tree; the nativeKey is persisted
	 * to the node so the provider can address it.
	 */
	public static ExternalEntry externalFile(final String nativeKey, final String relativePath, final Long size, final Long lastModified) {
		return new ExternalEntry(relativePath, null, false, size, lastModified, nativeKey, true);
	}

	public ExternalEntry withNativeKey(final String nativeKey) {
		return new ExternalEntry(relativePath, nodeUuid, directory, size, lastModified, nativeKey, bindNativeKey);
	}

	public boolean hasPath() {
		return relativePath != null;
	}

	public boolean hasUuid() {
		return nodeUuid != null;
	}

	/**
	 * @return the last segment of the relative path, or null for path-less entries
	 */
	public String name() {

		if (relativePath == null) {
			return null;
		}

		final int index = relativePath.lastIndexOf('/');

		return index >= 0 ? relativePath.substring(index + 1) : relativePath;
	}

	/**
	 * @return the '/'-separated parent path, "" for top-level entries, or null for path-less entries
	 */
	public String parentPath() {

		if (relativePath == null) {
			return null;
		}

		final int index = relativePath.lastIndexOf('/');

		return index >= 0 ? relativePath.substring(0, index) : "";
	}

	// ----- private methods -----
	private static String normalize(final String path) {

		if (path == null) {
			return null;
		}

		String normalized = path.replace('\\', '/');

		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}

		while (normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}

		return normalized;
	}
}
