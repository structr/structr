package org.structr.web.entity.news.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Principal;
import org.structr.web.common.RelType;
import org.structr.web.entity.news.NewsTickerItem;

/**
 *
 * @author Christian Morgner
 */
public class NewsAuthor extends OneToMany<Principal, NewsTickerItem> {

	@Override
	public Class<Principal> getSourceType() {
		return Principal.class;
	}

	@Override
	public Class<NewsTickerItem> getTargetType() {
		return NewsTickerItem.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.AUTHOR;
	}
}
