package org.structr.web.entity.relation;

import org.structr.core.entity.OneToMany;
import org.structr.core.property.Property;
import org.structr.web.entity.Image;

/**
 *
 * @author Christian Morgner
 */
public class Thumbnails extends OneToMany<Image, Image> {

	@Override
	public Class<Image> getSourceType() {
		return Image.class;
	}

	@Override
	public Class<Image> getTargetType() {
		return Image.class;
	}

	@Override
	public String name() {
		return "THUMBNAIL";
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
