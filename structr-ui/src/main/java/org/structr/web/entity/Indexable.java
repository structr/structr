/**
 * Copyright (C) 2010-2015 Structr GmbH
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

import java.io.InputStream;
import org.structr.common.PropertyView;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeService;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

//~--- interfaces -------------------------------------------------------------

/**
 *
 *
 */
public interface Indexable extends NodeInterface {

	public static final Property<String> contentType             = new StringProperty("contentType").indexedWhenEmpty();
	public static final Property<String> indexedContent          = new StringProperty("indexedContent").indexed(NodeService.NodeIndex.fulltext);
	public static final Property<String> extractedContent        = new StringProperty("extractedContent");
	public static final Property<String> indexedWords            = new StringProperty("indexedWords");

	public static final org.structr.common.View uiView = new org.structr.common.View(Indexable.class, PropertyView.Ui, contentType, indexedContent, extractedContent, indexedWords);
	
	public InputStream getInputStream();

	@Export
	public GraphObject getSearchContext(final String searchTerm, final int contextLength);
	
}
