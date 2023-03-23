/*
 * Copyright (C) 2010-2023 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonReferenceType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.schema.SchemaService;
import org.structr.web.entity.dom.DOMElement;

import java.net.URI;

/**
 * This class represents elements which can have an outgoing link to a resource.
 */
public interface LinkSource extends DOMElement {

	static class Impl { static {

		final JsonSchema schema       = SchemaService.getDynamicSchema();
		final JsonObjectType type     = schema.addType("LinkSource");
		final JsonObjectType linkable = (JsonObjectType)schema.getType("Linkable");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/LinkSource"));
		type.setExtends(URI.create("#/definitions/DOMElement"));
		type.setCategory("ui");

		type.overrideMethod("getLinkable", false, "return getProperty(linkableProperty);");

		type.addMethod("setLinkable")
			.setSource("setProperty(linkableProperty, (Linkable)linkable);")
			.addException(FrameworkException.class.getName())
			.addParameter("linkable", "org.structr.web.entity.Linkable");

		final JsonReferenceType rel = type.relate(linkable, "LINK", Cardinality.ManyToOne, "linkingElements", "linkable");

		type.addIdReferenceProperty("linkableId", rel.getTargetProperty());
		linkable.addIdReferenceProperty("linkingElementsIds", rel.getSourceProperty());

		// view configuration
		type.addViewProperty(PropertyView.Ui, "children");
		type.addViewProperty(PropertyView.Ui, "linkable");
		type.addViewProperty(PropertyView.Ui, "linkableId");
	}}

	Linkable getLinkable();
	void setLinkable(final Linkable linkable) throws FrameworkException;
}
