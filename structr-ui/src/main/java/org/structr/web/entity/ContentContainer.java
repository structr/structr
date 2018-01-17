/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.net.URI;
import java.util.List;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.core.entity.Relation.Cardinality;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.property.ContentPathProperty;

/**
 * Base class for all content containers.
 */
public interface ContentContainer extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("ContentContainer");
		final JsonObjectType item = schema.addType("ContentItem");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/ContentContainer"));

		type.addBooleanProperty("isContentContainer", PropertyView.Public).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		type.addCustomProperty("path", ContentPathProperty.class.getName(), PropertyView.Public).setIndexed(true).setReadOnly(true);

		type.addPropertyGetter("parent", ContentContainer.class);
		type.addPropertyGetter("items", List.class);

		type.relate(type, "CONTAINS", Cardinality.OneToMany,  "parent",     "childContainers");
		type.relate(item, "CONTAINS", Cardinality.ManyToMany, "containers", "items");

		type.addViewProperty(PropertyView.Public, "childContainersProperty");
		type.addViewProperty(PropertyView.Public, "itemsProperty");
	}}

	ContentContainer getParent();
	List<ContentItem> getItems();


	/*

	public static final Property<List<ContentItem>>      items              = new EndNodes<>("items", ContainerContentItems.class);
	public static final Property<ContentContainer>       parent             = new StartNode<>("parent", ContainerContentContainer.class);
	public static final Property<List<ContentContainer>> childContainers    = new EndNodes<>("childContainers", ContainerContentContainer.class);
	public static final Property<Boolean>                isContentContainer = new ConstantBooleanProperty("isContentContainer", true);
	public static final Property<String>                 path               = new ContentPathProperty("path").indexed().readOnly();

	public static final View publicView = new View(Folder.class, PropertyView.Public, id, type, name, path, owner, items, parent, childContainers, isContentContainer);
	public static final View uiView     = new View(Folder.class, PropertyView.Ui, id, type, name, path, owner, items, parent, childContainers, isContentContainer);

	*/
}
