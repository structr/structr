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



package org.structr.rest.resource;

import org.structr.core.GraphObject;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Utility for paging
 *
 * @author Axel Morgner
 */
public class PagingHelper {

	private static final Logger logger = Logger.getLogger(PagingHelper.class.getName());

	//~--- methods --------------------------------------------------------

//      @Override
//      public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
//              return false;
//      }
//
//      @Override
//      public List<? extends GraphObject> doGet(String sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException {
//
//              List<? extends GraphObject> results = wrappedResource.doGet(sortKey, sortDescending, pageSize, page);
//              
//              return subList(results, pageSize, page);
//      }
//
//      @Override
//      public Resource tryCombineWith(Resource next) throws FrameworkException {
//              return super.tryCombineWith(next);
//      }

	/**
	 * Return a single page of the list with the given paging parameters.
	 *
	 * @param list
	 * @param pageSize
	 * @param page
	 * @return
	 */
	public static List<? extends GraphObject> subList(final List<? extends GraphObject> list, int pageSize, int page) {

		if (pageSize <= 0) {

			return list;
		}

		int size      = list.size();
		int fromIndex = Math.min(size, Math.max(0, (page - 1) * pageSize));
		int toIndex   = Math.min(size, page * pageSize);

		return list.subList(fromIndex, toIndex);

	}

	public static Result postProcessResultSet(Result result, int resultCount, int pageSize, int page) {

		if (resultCount > 0) {

			result.setResultCount(resultCount);
		}

		if (page > 0) {

			result.setPage(page);
		}

		if (pageSize > 0) {

			result.setPageSize(pageSize);
		}

		int pageCount = getPageCount(resultCount, pageSize);

		if (pageCount > 0) {

			result.setPageCount(pageCount);
		}

		return result;

	}

	//~--- get methods ----------------------------------------------------

	private static int getPageCount(int resultCount, int pageSize) {

		return (int) Math.rint(Math.ceil((double) resultCount / (double) pageSize));

	}

//
//      @Override
//      public String getResourceSignature() {
//            
//            String uriPart    = getUriPart();
//            StringBuilder uri = new StringBuilder();
//
//            if (uriPart.contains("/")) {
//
//                    String[] parts = StringUtils.split(uriPart, "/");
//
//                    for (String subPart : parts) {
//
//                            if (!subPart.matches("[a-zA-Z0-9]{32}")) {
//
//                                    uri.append(subPart);
//                                    uri.append("/");
//
//                            }
//
//                    }
//
//                    return uri.toString();
//
//            } else {
//
//                    return uriPart;
//
//            }
//      }

}
