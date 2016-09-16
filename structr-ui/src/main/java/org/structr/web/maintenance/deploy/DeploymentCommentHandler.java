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
package org.structr.web.maintenance.deploy;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.importer.CommentHandler;

/**
 *
 */
public class DeploymentCommentHandler implements CommentHandler {

	@Override
	public void handleComment(final Page page, final DOMNode node, final String comment) throws FrameworkException {

		// implement special comment syntax here to modify the given node
		final String trimmedComment = comment.trim();

		if (StringUtils.isNotBlank(trimmedComment)) {

			if (trimmedComment.contains("@structr:private")) {

				node.setProperty(AbstractNode.visibleToPublicUsers, false);
				node.setProperty(AbstractNode.visibleToAuthenticatedUsers, false);
			}

			if (trimmedComment.contains("@structr:protected")) {
				node.setProperty(AbstractNode.visibleToPublicUsers, false);
				node.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
			}

			if (trimmedComment.contains("@structr:public")) {
				node.setProperty(AbstractNode.visibleToPublicUsers, true);
				node.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
			}

			if (trimmedComment.contains("@structr:public-only")) {
				node.setProperty(AbstractNode.visibleToPublicUsers, true);
				node.setProperty(AbstractNode.visibleToAuthenticatedUsers, false);
			}
		}
	}
}
