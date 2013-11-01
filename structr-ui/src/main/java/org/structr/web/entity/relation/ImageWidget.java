package org.structr.web.entity.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.OneToMany;
import org.structr.web.common.RelType;
import org.structr.web.entity.Image;
import org.structr.web.entity.Widget;

/**
 *
 * @author Christian Morgner
 */
public class ImageWidget extends OneToMany<Widget, Image> {

	@Override
	public Class<Image> getTargetType() {
		return Image.class;
	}

	@Override
	public Class<Widget> getSourceType() {
		return Widget.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.PICTURE_OF;
	}
}
