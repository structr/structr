/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
package org.structr.web.entity.blog.relation;

import org.structr.web.entity.blog.BlogComment;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Principal;

/**
 *
 * @author Christian Morgner
 */
public class BlogAuthor extends OneToMany<Principal, BlogComment> {

	@Override
	public Class<Principal> getSourceType() {
		return Principal.class;
	}

	@Override
	public Class<BlogComment> getTargetType() {
		return BlogComment.class;
	}

	@Override
	public String name() {
		return "AUTHOR";
	}
}
