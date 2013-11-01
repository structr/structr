package org.structr.web.entity.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.entity.ManyToOne;
import org.structr.web.common.RelType;
import org.structr.web.entity.Image;
import org.structr.web.entity.Widget;

/**
 *
 * @author Christian Morgner
 */
public class WidgetImage extends ManyToOne<Image, Widget> {

	@Override
	public Class<Widget> getTargetType() {
		return Widget.class;
	}

	@Override
	public Class<Image> getSourceType() {
		return Image.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.PICTURE_OF;
	}
}
