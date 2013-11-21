package org.structr.web.entity.relation;

import org.structr.core.entity.ManyToMany;
import org.structr.core.property.Property;
import org.structr.web.entity.Tag;
import org.structr.web.entity.Taggable;

/**
 *
 * @author Christian Morgner
 */
public class Tagging extends ManyToMany<Tag, Taggable> {

	@Override
	public Class<Tag> getSourceType() {
		return Tag.class;
	}

	@Override
	public Class<Taggable> getTargetType() {
		return Taggable.class;
	}

	@Override
	public String name() {
		return "TAG";
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
