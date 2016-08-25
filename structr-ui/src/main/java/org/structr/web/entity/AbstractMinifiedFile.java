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
package org.structr.web.entity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.dynamic.File;
import org.structr.web.entity.relation.MinificationNeighbor;

/**
 * Base class for minifiable files in structr
 *
 */
public abstract class AbstractMinifiedFile extends File {

	private static final Logger logger = Logger.getLogger(AbstractMinifiedFile.class.getName());

	public static final Property<List<FileBase>> minificationSources = new EndNodes<>("minificationSources", MinificationNeighbor.class);

	@Override
	public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		try {
			this.minify();
		} catch (IOException ex) {
			logger.log(Level.WARNING, "Could not automatically minify file", ex);
		}

		return super.onModification(securityContext, errorBuffer);
	}

	@Export
	public abstract void minify() throws FrameworkException, IOException;

	public int getMaxPosition () {
		int max = 0;
		for (final MinificationNeighbor neighbor : getOutgoingRelationships(MinificationNeighbor.class)) {
			max = Math.max(max, neighbor.getProperty(MinificationNeighbor.position));
		}
		return max;
	}

	public String getConcatenatedSource () throws FrameworkException, IOException {
		final StringBuilder concatenatedSource = new StringBuilder();

		int cnt = 0;
		for (MinificationNeighbor rel : getSortedRelationships()) {
			final FileBase src = rel.getTargetNode();

			concatenatedSource.append(FileUtils.readFileToString(src.getFileOnDisk()));

			// compact the relationships (if necessary)
			rel.setProperty(MinificationNeighbor.position, cnt++);
		}

		return concatenatedSource.toString();
	}

	public List<MinificationNeighbor> getSortedRelationships() {
		final List<MinificationNeighbor> rels = new ArrayList();
		getOutgoingRelationships(MinificationNeighbor.class).forEach(rels::add);

		Collections.sort(rels, (MinificationNeighbor arg0, MinificationNeighbor arg1) -> (arg0.getProperty(MinificationNeighbor.position).compareTo(arg1.getProperty(MinificationNeighbor.position))));

		return rels;
	}

	/**
	 * Move a minification source to a new position.
	 * All minification sources between those positions have to be adjusted as well.
	 *
	 * @param from The position from where the minification source is moved
	 * @param to The position where to move the minification source
	 * @throws FrameworkException
	 */
	@Export
	public void moveMinificationNeighbor(final int from, final int to) throws FrameworkException {

		for (MinificationNeighbor rel : getOutgoingRelationships(MinificationNeighbor.class)) {

			int currentPosition = rel.getProperty(MinificationNeighbor.position);

			int change = 0;
			if (from < to) {
				change = -1;
			} else if (from > to) {
				change = 1;
			}

			if (currentPosition > from && currentPosition <= to) {

				rel.setProperty(MinificationNeighbor.position, currentPosition + change);

			} else if (currentPosition >= to && currentPosition < from) {

				rel.setProperty(MinificationNeighbor.position, currentPosition + change);

			} else if (currentPosition == from) {

				rel.setProperty(MinificationNeighbor.position, to);

			}

		}

	}

}
