package org.structr.web.entity.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.web.common.RelType;
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
	public RelationshipType getRelationshipType() {
		return RelType.THUMBNAIL;
	}
}
