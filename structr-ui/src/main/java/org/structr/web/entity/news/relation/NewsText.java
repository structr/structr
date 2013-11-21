package org.structr.web.entity.news.relation;

import org.structr.core.entity.OneToOne;
import org.structr.core.property.Property;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.news.NewsTickerItem;

/**
 *
 * @author Christian Morgner
 */
public class NewsText extends OneToOne<NewsTickerItem, Content> {

	@Override
	public Class<NewsTickerItem> getSourceType() {
		return NewsTickerItem.class;
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
