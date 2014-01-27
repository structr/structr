/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.news.relation;

import org.structr.core.entity.OneToOne;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.news.NewsTickerItem;

/**
 *
 * @author Christian Morgner
 */
public class NewsText extends OneToOne<NewsTickerItem, Content> {

	@Override
	public Class<NewsTickerItem> getSourceType() {
		return NewsTickerItem.class;
	}

	@Override
	public Class<Content> getTargetType() {
		return Content.class;
	}

	@Override
	public String name() {
		return "CONTAINS";
	}
}
