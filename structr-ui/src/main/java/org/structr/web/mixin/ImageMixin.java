package org.structr.web.mixin;

import org.structr.core.mixin.Mixin;

/**
 *
 * @author Christian Morgner
 */
public class ImageMixin implements Mixin {

	@Override
	public String getType() {
		return "Image";
	}

	@Override
	public String getSuperclass() {
		return "org.structr.dynamic.File";
	}
}
