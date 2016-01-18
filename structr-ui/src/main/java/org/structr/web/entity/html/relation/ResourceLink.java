/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.html.relation;

import org.structr.core.property.Property;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.ManyToOne;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;

//~--- classes ----------------------------------------------------------------

/**
 *
 *
 */
public class ResourceLink extends ManyToOne<LinkSource, Linkable> {

	public static final View uiView = new View(ResourceLink.class, PropertyView.Ui,
		sourceId, targetId, type
	);

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

	@Override
	public boolean isInternal() {
		return true;
	}
}
