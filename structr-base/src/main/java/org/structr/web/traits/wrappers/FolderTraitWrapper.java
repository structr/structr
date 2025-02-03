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
package org.structr.web.traits.wrappers;

import org.structr.api.util.Iterables;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.files.external.DirectoryWatchService;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;

import java.util.LinkedList;
import java.util.List;

public class FolderTraitWrapper extends AbstractFileTraitWrapper implements Folder {

	public FolderTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getMountTarget() {
		return wrappedObject.getProperty(traits.key("mountTarget"));
	}

	@Override
	public String getMountTargetFileType() {
		return wrappedObject.getProperty(traits.key("mountTargetFileType"));
	}

	@Override
	public String getMountTargetFolderType() {
		return wrappedObject.getProperty(traits.key("mountTargetFolderType"));
	}

	@Override
	public String getEnabledChecksums() {
		return wrappedObject.getProperty(traits.key("enabledChecksums"));
	}

	@Override
	public Iterable<AbstractFile> getChildren() {

		final PropertyKey<Iterable<NodeInterface>> childrenKey = traits.key("children");

		// wrap nodes in AbstractFile wrapper
		return Iterables.map(c -> c.as(AbstractFile.class), wrappedObject.getProperty(childrenKey));
	}

	@Override
	public Iterable<File> getFiles() {
		// fixme: this wont work with the new implementation!
		return Iterables.map(s -> (File)s, Iterables.filter((AbstractFile value) -> value.is("File"), getChildren()));
	}

	@Override
	public Iterable<Folder> getFolders() {
		// fixme: this wont work with the new implementation!
		return Iterables.map(s -> (Folder)s, Iterables.filter((AbstractFile value) -> value.is("folder"), getChildren()));
	}

	@Override
	public Iterable<Image> getImages() {
		// fixme: this wont work with the new implementation!
		return Iterables.map(s -> (Image)s, Iterables.filter((AbstractFile value) -> value.is("Image"), getChildren()));
	}

	@Override
	public List<NodeInterface> getAllChildNodes() {

		final List<NodeInterface> allChildren = new LinkedList<>();

		for (AbstractFile child : getFiles()) {

			allChildren.add(child.getWrappedNode());
		}

		for (Folder child : getFolders()) {

			allChildren.add(child.getWrappedNode());

			allChildren.addAll(child.getAllChildNodes());
		}

		return allChildren;
	}

	static void updateWatchService(final Folder thisFolder, final boolean mount) {

		if (Services.getInstance().isConfigured(DirectoryWatchService.class)) {

			final DirectoryWatchService service = StructrApp.getInstance().getService(DirectoryWatchService.class);
			if (service != null && service.isRunning()) {

				if (mount) {

					service.mountFolder(thisFolder);

				} else {

					service.unmountFolder(thisFolder);
				}
			}
		}
	}
}
