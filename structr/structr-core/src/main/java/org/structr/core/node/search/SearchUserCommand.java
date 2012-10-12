/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.node.search;

import java.util.Map;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.structr.common.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.NodeServiceCommand;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Principal;

//~--- classes ----------------------------------------------------------------

/**
 * Searches for a user node by her/his name in the database and returns the result.
 *
 * @author amorgner
 */
public class SearchUserCommand extends NodeServiceCommand {

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		final Command findNode             = Services.command(securityContext, FindNodeCommand.class);

		switch (parameters.length) {

			case 1 : {
				final Index<Node> index = getIndexFromArguments(NodeIndex.user, arguments);

				// we have only a simple user name
				if (parameters[0] instanceof String) {

					final String userName = (String) parameters[0];

					for (final Node n : index.get(AbstractNode.Key.name.name(), userName)) {

						final AbstractNode s = (AbstractNode) findNode.execute(n);

						if (s.getType().equals(Principal.class.getSimpleName())) {

							return s;

						}

					}

				}
			} break;

			case 3 : {

				final String userNickName = (String) parameters[0];
				final PropertyKey key = (PropertyKey) parameters[1];
				final NodeIndex idx = (NodeIndex) parameters[2];
				final Index<Node> index = getIndexFromArguments(idx, arguments);

				// see: http://docs.neo4j.org/chunked/milestone/indexing-create-advanced.html
				final IndexHits<Node> indexHits = index.query( key.name(), "\"" + userNickName + "\"" );
				try {
					for (final Node n : indexHits) {
						final Object u = findNode.execute(n);
						if (u != null) {
							return u;
						}
					}
				} finally {
					indexHits.close();
				}
			}	break;

			default :
				break;

		}


		return null;
	}

	@SuppressWarnings("unchecked")
	private Index<Node> getIndexFromArguments(final NodeIndex idx, final Map<String, Object> args) {
		final Index<Node> index = (Index<Node>) args.get(idx.name());
		return index;
	}
}
