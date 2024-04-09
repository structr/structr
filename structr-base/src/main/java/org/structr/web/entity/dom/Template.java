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
package org.structr.web.entity.dom;

import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.RelationshipInterface;
import org.structr.schema.SchemaService;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;

import java.net.URI;
import java.util.List;

public interface Template extends Content {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Template");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Template"));
		type.setExtends(URI.create("#/definitions/Content"));
		type.setCategory("ui");

		type.addStringProperty("contentType", PropertyView.Public).setIndexed(true);
		type.addStringProperty("content",     PropertyView.Public).setIndexed(true);

		type.overrideMethod("renderContent",  false, Template.class.getName() + ".renderContent(this, arg0, arg1);");
		type.overrideMethod("getContextName", false, "return StringUtils.defaultString(getProperty(AbstractNode.name), \"template\");");

		// view configuration
		type.addViewProperty(PropertyView.Public, "children");
		type.addViewProperty(PropertyView.Public, "childrenIds");

		type.addViewProperty(PropertyView.Ui, "children");
		type.addViewProperty(PropertyView.Ui, "childrenIds");

	}}

	public static void renderContent(final Template thisTemplate, final RenderContext renderContext, final int depth) throws FrameworkException {

		final SecurityContext securityContext = thisTemplate.getSecurityContext();
		final EditMode editMode               = renderContext.getEditMode(securityContext.getUser(false));

		if (EditMode.DEPLOYMENT.equals(editMode)) {

			final DOMNode _syncedNode = thisTemplate.getSharedComponent();
			final AsyncBuffer out     = renderContext.getBuffer();

			if (depth > 0) {
				out.append(DOMNode.indent(depth, renderContext));
			}

			DOMNode.renderDeploymentExportComments(thisTemplate, out, true);

			out.append("<structr:template src=\"");

			if (_syncedNode != null) {

				// use name of synced node
				final String _name = _syncedNode.getProperty(AbstractNode.name);
				out.append(_name != null ? _name.concat("-").concat(_syncedNode.getUuid()) : _syncedNode.getUuid());

			} else {

				// use name of local template
				final String _name = thisTemplate.getProperty(AbstractNode.name);
				out.append(_name != null ? _name.concat("-").concat(thisTemplate.getUuid()) : thisTemplate.getUuid());
			}

			out.append("\"");

			DOMNode.renderSharedComponentConfiguration(thisTemplate, out, editMode);
			DOMNode.renderCustomAttributes(thisTemplate, out, securityContext, renderContext); // include custom attributes in templates as well!

			out.append(">");

			// fetch children
			final List<RelationshipInterface> rels = thisTemplate.getChildRelationships();
			if (rels.isEmpty()) {

				// No child relationships, maybe this node is in sync with another node
				if (_syncedNode != null) {
					rels.addAll(_syncedNode.getChildRelationships());
				}
			}

			for (final RelationshipInterface rel : rels) {

				final DOMNode subNode = (DOMNode) rel.getTargetNode();
				subNode.render(renderContext, depth + 1);
			}

			out.append(DOMNode.indent(depth, renderContext));
			out.append("</structr:template>");
			out.append(DOMNode.indent(depth-1, renderContext));

		} else if (EditMode.SHAPES.equals(editMode)) {

			final AsyncBuffer out = renderContext.getBuffer();

			out.append("<structr:template data-structr-id=\"");
			out.append(thisTemplate.getUuid());
			out.append("\">\n");

			// render content
			Content.renderContent(thisTemplate, renderContext, depth);

			out.append("\n</structr:template>\n");

		} else if (EditMode.SHAPES_MINIATURES.equals(editMode)) {

			final AsyncBuffer out = renderContext.getBuffer();

			out.append("<structr:template data-structr-id=\"");
			out.append(thisTemplate.getUuid());
			out.append("\">\n");

			// Append preview CSS
			out.append("<style type=\"text/css\">");
			out.append(thisTemplate.getProperty("previewCss"));
			out.append("</style>\n");

			// render content
			Content.renderContent(thisTemplate, renderContext, depth);

			out.append("\n</structr:template>\n");

		} else {

			// "super" call using static method..
			Content.renderContent(thisTemplate, renderContext, depth);
		}
	}
}
