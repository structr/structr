/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.common;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;

/**
 * Utility to merge to graphs.
 *
 *
 */
public class GraphMergeHelper {

	private static final Logger logger = Logger.getLogger(GraphMergeHelper.class.getName());

	/**
	 * Merge new nodes into original nodes including all relationships and
	 * properties
	 *
	 * @param <T>
	 * @param newNodes
	 * @param origNodes
	 * @param shadowIdPropertyKey
	 */
	public static <T extends NodeInterface> void merge(final Set<T> origNodes, final Set<T> newNodes, final PropertyKey shadowIdPropertyKey) {

		final App app = StructrApp.getInstance();
		
		try (final Tx tx = app.tx()) {

			// Compare uuid of original nodes with the id given in the shadow id property
			// and mark all original nodes as deleted which are not contained in new nodes list anymore
			for (final NodeInterface origNode : origNodes) {

				origNode.setProperty(NodeInterface.deleted, true);
				
				for (final NodeInterface newNode : newNodes) {
				
					final String shadowId = (String) newNode.getProperty(shadowIdPropertyKey);
					logger.log(Level.INFO, "New node shadow id: {0}", shadowId);
					
					if (origNode.getUuid().equals(shadowId)) {
						origNode.setProperty(NodeInterface.deleted, false);
					}
				
				}

			}

			// Delete all original nodes which are marked as deleted
			for (final NodeInterface origNode : origNodes) {

				if (origNode.getProperty(NodeInterface.deleted)) {
				
					app.delete(origNode);
				
				}
				

			}

			// Delete all new nodes
			for (final NodeInterface newNode : newNodes) {

				app.delete(newNode);

			}

			tx.success();
			
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

	}
}
