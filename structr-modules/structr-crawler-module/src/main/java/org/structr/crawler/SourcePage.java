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
package org.structr.crawler;

import org.structr.common.View;
import org.structr.core.property.*;
import static org.structr.core.entity.SchemaRelationshipNode.name;

public class SourcePage extends CrawlerTreeNode {

	public static final Property<java.util.List<SourcePattern>> patternsProperty = new EndNodes<>("patterns", SourcePageUSESourcePattern.class).dynamic();
	public static final Property<java.util.List<SourcePage>> subPagesProperty = new EndNodes<>("subPages", SourcePageSUBSourcePage.class).dynamic();
	public static final Property<SourceSite> siteProperty = new StartNode<>("site", SourceSiteCONTAINSSourcePage.class).dynamic();
	public static final Property<SourcePage> parentPageProperty = new StartNode<>("parentPage", SourcePageSUBSourcePage.class).dynamic();
	public static final Property<SourcePattern> subPageOfProperty = new StartNode<>("subPageOf", SourcePatternSUBPAGESourcePage.class).dynamic();
	public static final Property<java.lang.String> urlProperty = new StringProperty("url").indexed().dynamic();

	public static final View uiView = new View(SourcePage.class, "ui",
		patternsProperty, subPagesProperty, siteProperty, parentPageProperty, subPageOfProperty, urlProperty
	);

	public static final View publicView = new View(SourcePage.class, "public",
		name, type, id
	);


}
