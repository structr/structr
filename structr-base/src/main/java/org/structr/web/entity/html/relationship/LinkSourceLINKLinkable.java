/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.entity.html.relationship;

import org.structr.core.entity.ManyToOne;
import org.structr.web.entity.LinkSource;
import org.structr.web.entity.Linkable;

public class LinkSourceLINKLinkable extends ManyToOne<LinkSource, Linkable> {

	@Override
	public Class<LinkSource> getSourceType() {
		return LinkSource.class;
	}

	@Override
	public Class<Linkable> getTargetType() {
		return Linkable.class;
	}

	@Override
	public String name() {
		return "LINK";
	}
}
