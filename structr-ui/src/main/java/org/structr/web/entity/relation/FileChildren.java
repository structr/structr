package org.structr.web.entity.relation;

import org.structr.core.entity.relationship.AbstractChildren;
import org.structr.core.property.Property;
import org.structr.web.entity.AbstractFile;

/**
 *
 * @author Christian Morgner
 */
public class FileChildren extends AbstractChildren<AbstractFile, AbstractFile> {

	@Override
	public Class<AbstractFile> getSourceType() {
		return AbstractFile.class;
	}

	@Override
	public Class<AbstractFile> getTargetType() {
		return AbstractFile.class;
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return null;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return null;
	}
}
