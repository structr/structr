/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.text.model;

import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.text.model.relationship.StructuredDocumentMETADATAMetadataNode;

/**
 *
 */
public class StructuredDocument extends StructuredTextNode {

	public static final Property<Iterable<MetadataNode>> metadataProperty = new EndNodes<>("metadata", StructuredDocumentMETADATAMetadataNode.class);

	public static final Property<String> titleProperty  = new StringProperty("title");
	public static final Property<String> authorProperty = new StringProperty("author");
	public static final Property<String> statusProperty = new StringProperty("status");

	public Iterable<MetadataNode> getMetadata() {
		return getProperty(metadataProperty);
	}
}
