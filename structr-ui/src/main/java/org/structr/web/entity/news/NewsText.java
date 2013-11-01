package org.structr.web.entity.news;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToOne;
import org.structr.web.common.RelType;
import org.structr.web.entity.dom.Content;

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
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS;
	}
}
