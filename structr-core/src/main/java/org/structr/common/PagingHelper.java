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

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.GraphObject;
import org.structr.core.Result;

//~--- classes ----------------------------------------------------------------

/**
 * Utility for paging
 *
 *
 */
public class PagingHelper {

	private static final Logger logger = Logger.getLogger(PagingHelper.class.getName());

	//~--- methods --------------------------------------------------------

	/**
	 * Return a single page of the list with the given paging parameters.
	 *
	 * @param list
	 * @param pageSize
	 * @param page
	 * @param offsetId
	 * @return subList
	 */
	public static List<? extends GraphObject> subList(final List<? extends GraphObject> list, int pageSize, int page, String offsetId) {

		if (pageSize <= 0 || page == 0) {

			return Collections.EMPTY_LIST;
		}

		int size        = list.size();
		int fromIndex;
		int toIndex;
		if (StringUtils.isNotBlank(offsetId)) {

			int offsetIndex = 0;
			int i=0;
			for (GraphObject obj : list) {
				if (obj.getUuid().equals(offsetId)) {
					offsetIndex = i;
					break;
				} else {
					i++;
					continue;
				}
			}
			
			fromIndex = page > 0
				     ? offsetIndex
				     : offsetIndex + (page * pageSize);
			
		} else {
		
			fromIndex   = page > 0
				     ? (page - 1) * pageSize
				     : size + (page * pageSize);
			
			
		}

		toIndex     = fromIndex + pageSize;
		
		int finalFromIndex = Math.max(0, fromIndex);
		int finalToIndex   =  Math.min(size, Math.max(0, toIndex));

		// prevent fromIndex to be greater than toIndex
		if (finalFromIndex > finalToIndex) {
			finalFromIndex = finalToIndex;
		}

		try {
			return list.subList(finalFromIndex, finalToIndex);
			
		} catch (Throwable t) {
			
			logger.log(Level.WARNING, "Invalid range for sublist in paging, pageSize {0}, page {1}, offsetId {2}: {3}", new Object[] {
				pageSize,
				page,
				offsetId,
				t.getMessage()
			});
		}
		
		return Collections.EMPTY_LIST;

	}

	/**
	 * Return a single page of the result with the given paging parameters.
	 *
	 * @param result
	 * @param pageSize
	 * @param page
	 * @param offsetId
	 * @return subResult
	 */
	public static Result subResult(final Result result, int pageSize, int page, String offsetId) {

		if (pageSize <= 0 || page == 0) {

			return result;
		}

		int pageCount = getPageCount(result.getRawResultCount(), pageSize);

		if (pageCount > 0) {

			result.setPageCount(pageCount);
		}

		if (page > pageCount) {

			page = pageCount;
		}

		result.setPage(page);
		result.setPageSize(pageSize);

		return new Result(subList(result.getResults(), pageSize, page, offsetId), result.getResults().size(), result.isCollection(), result.isPrimitiveArray());

	}

	public static Result addPagingParameter(Result result, int pageSize, int page) {

		if (pageSize > 0 && pageSize < Integer.MAX_VALUE) {

			int pageCount = getPageCount(result.getRawResultCount(), pageSize);

			if (pageCount > 0) {

				result.setPageCount(pageCount);
			}

			result.setPage(page);
			result.setPageSize(pageSize);

		}

		return result;

	}

	//~--- get methods ----------------------------------------------------

	private static int getPageCount(int resultCount, int pageSize) {

		return (int) Math.rint(Math.ceil((double) resultCount / (double) pageSize));

	}

}
