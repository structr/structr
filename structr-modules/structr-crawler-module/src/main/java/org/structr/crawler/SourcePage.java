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
package org.structr.crawler;

import org.structr.common.View;
import org.structr.core.property.*;

public class SourcePage extends CrawlerTreeNode {

	public static final Property<Iterable<SourcePattern>> patterns    = new EndNodes<>("patterns",    SourcePageUSESourcePattern.class);
	public static final Property<Iterable<SourcePage>>    subPages    = new EndNodes<>("subPages",    SourcePageSUBSourcePage.class);
	public static final Property<SourceSite>              site        = new StartNode<>("site",       SourceSiteCONTAINSSourcePage.class);
	public static final Property<SourcePage>              parentPage  = new StartNode<>("parentPage", SourcePageSUBSourcePage.class);
	public static final Property<SourcePattern>           subPageOf   = new StartNode<>("subPageOf",  SourcePatternSUBPAGESourcePage.class);

	public static final Property<String>                  url         = new StringProperty("url").indexed();
	public static final Property<Boolean>                 isLoginPage = new BooleanProperty("isLoginPage").indexed();

	public static final View uiView = new View(SourcePage.class, "ui",
		patterns, subPages, site, parentPage, subPageOf, url, isLoginPage
	);

	public static final View publicView = new View(SourcePage.class, "public",
		name, type, id
	);

}
