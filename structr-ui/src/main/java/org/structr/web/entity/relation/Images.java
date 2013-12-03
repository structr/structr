package org.structr.web.entity.relation;

import org.structr.core.entity.relationship.AbstractChildren;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;

/**
 *
 * @author Christian Morgner
 */
public class Images extends AbstractChildren<Folder, Image> {

	@Override
	public Class<Folder> getSourceType() {
		return Folder.class;
	}

	@Override
	public Class<Image> getTargetType() {
		return Image.class;
	}
}
