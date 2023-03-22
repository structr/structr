package org.structr.core.storage;

import org.structr.web.entity.AbstractFile;

public abstract class AbstractStorageProvider implements StorageProvider {
	private final AbstractFile file;

	public AbstractStorageProvider(final AbstractFile file) {
		this.file = file;
	}

	public AbstractFile getFile() {
		return file;
	}
}
