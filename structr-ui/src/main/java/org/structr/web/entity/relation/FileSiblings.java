package org.structr.web.entity.relation;

import org.structr.core.entity.relationship.AbstractListSiblings;
import org.structr.web.entity.AbstractFile;

/**
 *
 * @author Christian Morgner
 */
public class FileSiblings extends AbstractListSiblings<AbstractFile, AbstractFile> {

	@Override
	public Class<AbstractFile> getSourceType() {
		return AbstractFile.class;
	}

	@Override
	public Class<AbstractFile> getTargetType() {
		return AbstractFile.class;
	}
}
