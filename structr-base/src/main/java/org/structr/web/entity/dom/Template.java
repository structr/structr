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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.RelationshipInterface;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.dom.relationship.DOMNodeCONTAINSDOMNode;

import java.util.List;

public class Template extends Content {

	public static final View defaultView = new View(Template.class, PropertyView.Public,
		contentProperty, contentTypeProperty, childrenProperty, childrenIdsProperty
	);

	public static final View uiView = new View(Template.class, PropertyView.Ui,
		childrenProperty, childrenIdsProperty
	);

	@Override
	public void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {

		final SecurityContext securityContext = this.getSecurityContext();
		final EditMode editMode               = renderContext.getEditMode(securityContext.getUser(false));

		if (EditMode.DEPLOYMENT.equals(editMode)) {

			final DOMNode _syncedNode = this.getSharedComponent();
			final AsyncBuffer out     = renderContext.getBuffer();

			if (depth > 0) {
				out.append(DOMNode.indent(depth, renderContext));
			}

			this.renderDeploymentExportComments(out, true);

			out.append("<structr:template src=\"");

			if (_syncedNode != null) {

				// use name of synced node
				final String _name = _syncedNode.getProperty(AbstractNode.name);
				out.append(_name != null ? _name.concat("-").concat(_syncedNode.getUuid()) : _syncedNode.getUuid());

			} else {

				// use name of local template
				final String _name = this.getProperty(AbstractNode.name);
				out.append(_name != null ? _name.concat("-").concat(this.getUuid()) : this.getUuid());
			}

			out.append("\"");

			this.renderSharedComponentConfiguration(out, editMode);
			this.renderCustomAttributes(out, securityContext, renderContext); // include custom attributes in templates as well!

			out.append(">");

			// fetch children
			final List<DOMNodeCONTAINSDOMNode> rels = this.getChildRelationships();
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

		} else if (EditMode.PREVIEW.equals(editMode)) {

			final AsyncBuffer out = renderContext.getBuffer();

			out.append("<structr:template data-structr-id=\"");
			out.append(this.getUuid());
			out.append("\">\n");

			// render content
			super.renderContent(renderContext, depth);

			out.append("\n</structr:template>\n");

		} else {

			// "super" call using static method..
			super.renderContent(renderContext, depth);
		}
	}

	@Override
	public String getContextName() {
		return StringUtils.defaultString(getProperty(AbstractNode.name), "template");
	}
}
