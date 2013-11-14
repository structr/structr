package org.structr.web.entity.relation;

import org.structr.core.entity.relationship.AbstractChildren;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;

/**
 *
 * @author Christian Morgner
 */
public class Files extends AbstractChildren<Folder, File> {

	@Override
	public Class<Folder> getSourceType() {
		return Folder.class;
	}

	@Override
	public Class<File> getTargetType() {
		return File.class;
	}
}
