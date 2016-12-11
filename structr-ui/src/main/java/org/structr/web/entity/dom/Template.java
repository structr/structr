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

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.dom.relationship.DOMChildren;



public class Template extends Content {

	public static final Property<String> configuration                                   = new StringProperty("configuration").indexed();

	public static final org.structr.common.View uiView                                   = new org.structr.common.View(Content.class, PropertyView.Ui,
		children, childrenIds, content, contentType, parent, pageId, hideOnDetail, hideOnIndex, sharedComponent, syncedNodes, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		showForLocales, hideForLocales, showConditions, hideConditions, isContent, configuration
	);

	public static final org.structr.common.View publicView                               = new org.structr.common.View(Content.class, PropertyView.Public,
		children, childrenIds, content, contentType, parent, pageId, hideOnDetail, hideOnIndex, sharedComponent, syncedNodes, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		showForLocales, hideForLocales, showConditions, hideConditions, isContent, configuration
	);

	@Override
	public void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {

		final EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));
		if (EditMode.DEPLOYMENT.equals(editMode)) {

			final DOMNode _syncedNode = (DOMNode) getProperty(sharedComponent);
			final AsyncBuffer out     = renderContext.getBuffer();

			if (depth > 0) {
				out.append(DOMNode.indent(depth, renderContext));
			}

			renderDeploymentExportComments(out, true);

			out.append("<structr:template src=\"");

			if (_syncedNode != null) {

				// use name of synced node
				final String _name = _syncedNode.getProperty(AbstractNode.name);
				out.append(_name != null ? _name : _syncedNode.getUuid());

			} else {

				// use name of local template
				final String _name = getProperty(AbstractNode.name);
				out.append(_name != null ? _name : getUuid());
			}

			out.append("\"");

			renderSharedComponentConfiguration(out, editMode);
			renderCustomAttributes(out, securityContext, renderContext); // include custom attributes in templates as well!

			out.append(">");

			// fetch children
			final List<DOMChildren> rels = getChildRelationships();
			if (rels.isEmpty()) {

				// No child relationships, maybe this node is in sync with another node
				if (_syncedNode != null) {
					rels.addAll(_syncedNode.getChildRelationships());
				}
			}

			for (final AbstractRelationship rel : rels) {

				final DOMNode subNode = (DOMNode) rel.getTargetNode();
				subNode.render(renderContext, depth + 1);
			}

			out.append(DOMNode.indent(depth, renderContext));
			out.append("</structr:template>");
			out.append(DOMNode.indent(depth-1, renderContext));

		} else {

			super.renderContent(renderContext, depth);
		}
	}
}
