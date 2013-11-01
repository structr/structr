/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity.html.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.View;
import org.structr.core.property.StringProperty;
import org.structr.core.entity.OneToOne;
import org.structr.web.entity.Linkable;
import org.structr.web.entity.html.Script;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class ScriptLink extends OneToOne<Script, Linkable> {

	public static final Property<String> sourceId = new StringProperty("sourceId");
	public static final Property<String> targetId = new StringProperty("targetId");
	public static final Property<String> type     = new StringProperty("type");

	public static final View uiView = new View(ScriptLink.class, PropertyView.Ui,
		sourceId, targetId, type	
	);
	
	static {

		// EntityContext.registerNamedRelation("resource_link", Hyperlink.class, Link.class, Linkable.class, RelType.LINK);
		// EntityContext.registerNamedRelation("hyperlink", Hyperlink.class, A.class, Linkable.class, RelType.LINK);
	}

	@Override
	public Class<Linkable> getTargetType() {
		return Linkable.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.LINK;
	}

	@Override
	public Class<Script> getSourceType() {
		return Script.class;
	}
}
