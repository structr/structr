/*
 *  Copyright (C) 2011 Axel Morgner
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

package org.structr.web.servlet;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.StructrNodeFactory;
import org.structr.web.common.RelType;

/**
 *
 * @author Christian Morgner
 */
public class HtmlServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(HtmlServlet.class.getName());

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {

		try {
			request.setCharacterEncoding("UTF-8");

			// initialize security context
			SecurityContext securityContext = SecurityContext.getInstance(this.getServletConfig(), request, AccessMode.Backend);

			// 1: find entry point (Resource)

			// 2: do a traversal and collect content

			// 3: output content



		} catch(Throwable t) {

			logger.log(Level.WARNING, "Exception while processing request", t);
		}
	}

	private String getContent(final SecurityContext securityContext, AbstractNode node) {

		final StringBuilder builder = new StringBuilder();

		TraversalDescription desc = Traversal.description();
		desc = desc.breadthFirst();
		desc = desc.relationships(RelType.CONTAINS, Direction.OUTGOING);
		desc = desc.uniqueness(Uniqueness.NODE_GLOBAL);
		desc = desc.evaluator(new Evaluator() {

				@Override
				public Evaluation evaluate(Path path) {

					return Evaluation.EXCLUDE_AND_PRUNE;
				}
			}
		);

		// do traversal
		for(Path path : desc.traverse(node.getNode())) {
			
		}

		return builder.toString();
	}
}
