/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.web.entity.dom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;

import java.net.URI;

/**
 *
 */
public interface Comment extends Content, org.w3c.dom.Comment, NonIndexed {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Comment");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Comment"));
		type.setExtends(URI.create("#/definitions/Content"));
		type.setCategory("ui");

		type.overrideMethod("onCreation", true,  "setProperty(contentTypeProperty, \"text/html\");");
		type.overrideMethod("render",     false, Comment.class.getName() + ".render(this, arg0, arg1);");
	}}

	static void render(final Comment comment, final RenderContext renderContext, final int depth) throws FrameworkException {

		String _content = comment.getContent();

		// Avoid rendering existing @structr comments since those comments are
		// created depending on the visibility settings of individual nodes. If
		// those comments are rendered, there will be duplicates in a round-
		// trip export/import test.
		if (!_content.contains("@structr:")) {

			try {

				final SecurityContext securityContext = comment.getSecurityContext();
				final RenderContext.EditMode edit = renderContext.getEditMode(securityContext.getUser(false));
				final AsyncBuffer buf = renderContext.getBuffer();

				if (RenderContext.EditMode.DEPLOYMENT.equals(edit)) {

					DOMNode.renderDeploymentExportComments(comment, buf, true);

					buf.append("<!--").append(DOMNode.escapeForHtml(_content)).append("-->");

				} else {

					_content = comment.getPropertyWithVariableReplacement(renderContext, StructrApp.key(Content.class, "content"));

					buf.append("<!--").append(_content).append("-->");
				}

			} catch (Throwable t) {

				// catch exception to prevent ugly status 500 error pages in frontend.
				final Logger logger = LoggerFactory.getLogger(Content.class);
				logger.error("", t);
			}
		}
	}
}
