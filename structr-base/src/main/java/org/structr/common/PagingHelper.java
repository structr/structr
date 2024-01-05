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
package org.structr.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Utility for pagination.
 */
public class PagingHelper {

	private static final Logger logger = LoggerFactory.getLogger(PagingHelper.class.getName());

	/**
	 * Return a single page of the list with the given paging parameters.
	 *
	 * @param list
	 * @param pageSize
	 * @param page
	 * @return subList
	 */
	public static <T> List<T> subList(final List<T> list, int pageSize, int page) {

		if (pageSize <= 0 || page == 0) {

			return Collections.EMPTY_LIST;
		}

		int size        = list.size();

		int fromIndex   = page > 0
		     ? (page - 1) * pageSize
		     : size + (page * pageSize);

		int toIndex = fromIndex + pageSize;

		int finalFromIndex = Math.max(0, fromIndex);
		int finalToIndex   =  Math.min(size, Math.max(0, toIndex));

		// prevent fromIndex to be greater than toIndex
		if (finalFromIndex > finalToIndex) {
			finalFromIndex = finalToIndex;
		}

		try {
			return list.subList(finalFromIndex, finalToIndex);

		} catch (Throwable t) {

			logger.warn("Invalid range for sublist in paging, pageSize {}, page {}: {}", new Object[] {
				pageSize,
				page,
				t.getMessage()
			});
		}

		return Collections.EMPTY_LIST;

	}
}
