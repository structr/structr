/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.net.URI;
import org.structr.common.error.FrameworkException;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.web.common.RenderContext;

/**
 *
 */
public interface Comment extends Content, org.w3c.dom.Comment, NonIndexed {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Comment");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Comment"));
		type.setExtends(URI.create("#/definitions/Content"));

		type.overrideMethod("onCreation", true,  "setProperty(contentTypeProperty, \"text/html\");");
		type.overrideMethod("render",     false, Comment.class.getName() + ".render(this, arg0, arg1);");
	}}

	static void render(final Comment comment, final RenderContext renderContext, final int depth) throws FrameworkException {

		final String _content = comment.getContent();

		// Avoid rendering existing @structr comments since those comments are
		// created depending on the visiblity settings of individual nodes. If
		// those comments are rendered, there will be duplicates in a round-
		// trip export/import test.
		if (!_content.contains("@structr:")) {

			renderContext.getBuffer().append("<!--").append(_content).append("-->");
		}

	}
}
