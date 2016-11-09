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
package org.structr.web.entity.dom;

import org.structr.common.error.FrameworkException;
import org.structr.schema.NonIndexed;
import org.structr.web.common.RenderContext;

/**
 *
 *
 */
public class Comment extends Content implements org.w3c.dom.Comment, NonIndexed {

	@Override
	public void render(RenderContext renderContext, int depth) throws FrameworkException {

		final String _content = getProperty(content);

		// Avoid rendering existing @structr comments since those comments are
		// created depending on the visiblity settings of individual nodes. If
		// those comments are rendered, there will be duplicates in a round-
		// trip export/import test.
		if (!_content.contains("@structr:")) {

			renderContext.getBuffer().append("<!--").append(_content).append("-->");
		}

	}
}
