package org.structr.web.entity.mail;

import org.structr.core.entity.OneToOne;
import org.structr.core.property.Property;
import org.structr.web.entity.dom.Content;

/**
 *
 * @author Christian Morgner
 */
public class TemplateText extends OneToOne<MailTemplate, Content> {

	@Override
	public Class<MailTemplate> getSourceType() {
		return MailTemplate.class;
	}

	@Override
	public Class<Content> getTargetType() {
		return Content.class;
	}

	@Override
	public String name() {
		return "CONTAINS";
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
