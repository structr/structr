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

/**
 * Content element that can act as an outer template.
 *
 *
 */


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

		if (EditMode.DEPLOYMENT.equals(renderContext.getEditMode(securityContext.getUser(false)))) {

			final DOMNode _syncedNode = (DOMNode) getProperty(sharedComponent);
			final AsyncBuffer out     = renderContext.getBuffer();

			if (depth > 0) {
				out.append(DOMNode.indent(depth, renderContext));
			}

			out.append("<structr:template src=\"");

			if (_syncedNode != null) {

				final String name = _syncedNode.getProperty(AbstractNode.name);
				out.append(name != null ? name : _syncedNode.getUuid());

			} else {

				out.append(getUuid());
			}

			out.append("\"");

			// include custom attributes in templates as well!
			renderCustomAttributes(out, securityContext, renderContext);

			out.append(">");

			// TODO: we need to include the children here...
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

		} else {

			super.renderContent(renderContext, depth);
		}
	}
}
