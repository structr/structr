package org.structr.web.mixin;

import org.structr.core.mixin.Mixin;
import org.structr.web.entity.AbstractFile;

/**
 *
 * @author Christian Morgner
 */
public class FileMixin implements Mixin {

	@Override
	public String getType() {
		return "File";
	}

	@Override
	public String getSuperclass() {
		return AbstractFile.class.getName();
	}

}
