/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html.relation;

import java.util.Collections;
import java.util.List;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.Syncable;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.ManyToOne;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class ResourceLink extends ManyToOne<LinkSource, Linkable> implements Syncable {

	public static final Property<String> sourceId = new StringProperty("sourceId");
	public static final Property<String> targetId = new StringProperty("targetId");
	public static final Property<String> type     = new StringProperty("type");

	public static final View uiView = new View(ResourceLink.class, PropertyView.Ui,
		sourceId, targetId, type
	);

	static {

		// StructrApp.getConfiguration().registerNamedRelation("resource_link", ResourceLink.class, Link.class, Linkable.class, RelType.LINK);
		// StructrApp.getConfiguration().registerNamedRelation("hyperlink", ResourceLink.class, A.class, Linkable.class, RelType.LINK);
	}

	@Override
	public Class<LinkSource> getSourceType() {
		return LinkSource.class;
	}

	@Override
	public String name() {
		return "LINK";
	}

	@Override
	public Class<Linkable> getTargetType() {
		return Linkable.class;
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return sourceId;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return targetId;
	}

	// ----- interface Syncable -----
	@Override
	public List<Syncable> getSyncData() {
		return Collections.emptyList();
	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public boolean isRelationship() {
		return true;
	}

	@Override
	public NodeInterface getSyncNode() {
		return null;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return this;
	}

	@Override
	public void updateFromPropertyMap(PropertyMap properties) throws FrameworkException {
	}
}
