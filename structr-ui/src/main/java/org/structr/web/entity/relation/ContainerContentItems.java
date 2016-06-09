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
package org.structr.web.entity.relation;

import org.structr.core.entity.ManyToMany;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.Property;
import org.structr.web.entity.ContentContainer;
import org.structr.web.entity.ContentItem;

/**
 *
 *
 */
public class ContainerContentItems extends ManyToMany<ContentContainer, ContentItem> {

	public static final Property<Boolean>        isContentContainer                = new ConstantBooleanProperty("isContentContainer", true);
	
	@Override
	public Class<ContentContainer> getSourceType() {
		return ContentContainer.class;
	}

	@Override
	public Class<ContentItem> getTargetType() {
		return ContentItem.class;
	}

	@Override
	public String name() {
		return "CONTAINS";
	}
}
