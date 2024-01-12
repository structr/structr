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
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

public class SourceSite extends CrawlerTreeNode {

	public static final Property<Iterable<SourcePage>> pages = new EndNodes<>("pages", SourceSiteCONTAINSSourcePage.class);
	public static final Property<String>        proxyUrl     = new StringProperty("proxyUrl");
	public static final Property<String>   proxyUsername     = new StringProperty("proxyUsername");
	public static final Property<String>   proxyPassword     = new StringProperty("proxyPassword");
	public static final Property<String>    authUsername     = new StringProperty("authUsername");
	public static final Property<String>    authPassword     = new StringProperty("authPassword");
	public static final Property<String>          cookie     = new StringProperty("cookie");

	public static final View uiView = new View(SourceSite.class, "ui",
		pages, proxyUrl, proxyUsername, proxyPassword, authUsername, authPassword, cookie
	);

}
