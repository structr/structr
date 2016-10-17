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
package org.structr.web.entity;

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.web.entity.relation.ContainerContentContainer;
import org.structr.web.entity.relation.ContainerContentItems;
import org.structr.web.property.ContentPathProperty;

/**
 * Base class for all content containers.
 */
public abstract class ContentContainer extends AbstractNode {

	public static final Property<List<ContentItem>>      items              = new EndNodes<>("items", ContainerContentItems.class);
	public static final Property<ContentContainer>       parent             = new StartNode<>("parent", ContainerContentContainer.class);
	public static final Property<List<ContentContainer>> childContainers    = new EndNodes<>("childContainers", ContainerContentContainer.class);
	public static final Property<Boolean>                isContentContainer = new ConstantBooleanProperty("isContentContainer", true);
	public static final Property<String>                 path               = new ContentPathProperty("path").indexed().readOnly();
	
	public static final View publicView = new View(Folder.class, PropertyView.Public, id, type, name, path, owner, items, parent, childContainers, isContentContainer);
	public static final View uiView     = new View(Folder.class, PropertyView.Ui, id, type, name, path, owner, items, parent, childContainers, isContentContainer);
	
}
