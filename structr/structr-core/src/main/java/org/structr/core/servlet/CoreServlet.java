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

package org.structr.core.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.AccessMode;
import org.structr.common.CurrentRequest;
import org.structr.core.entity.AbstractNode;
import org.structr.core.resource.AbstractNodeTypeAdapter;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.PathException;
import org.structr.core.resource.constraint.IdConstraint;
import org.structr.core.resource.constraint.PagingConstraint;
import org.structr.core.resource.constraint.ResourceConstraint;
import org.structr.core.resource.constraint.Result;
import org.structr.core.resource.constraint.RootResourceConstraint;
import org.structr.core.resource.constraint.SortConstraint;

/**
 *
 * @author Christian Morgner
 */
public class CoreServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(CoreServlet.class.getName());

	private static final String	REQUEST_PARAMETER_SORT_KEY =		"sort";
	private static final String	REQUEST_PARAMETER_SORT_ORDER =		"order";
	private static final String	REQUEST_PARAMETER_PAGE_NUMBER =		"page";
	private static final String	REQUEST_PARAMETER_PAGE_SIZE =		"pageSize";

	private static final int	DEFAULT_VALUE_PAGE_SIZE =		20;
	private static final String	DEFAULT_VALUE_SORT_ORDER =		"asc";

	private Map<Pattern, Class> constraints = null;
	private Gson gson = null;

	@Override
	public void init() {

		this.constraints = new LinkedHashMap<Pattern, Class>();
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(AbstractNode.class, new AbstractNodeTypeAdapter())
			.create();

		// initialize constraints
		constraints.put(Pattern.compile("[0-9]+"), IdConstraint.class);
	}

	@Override
	public void destroy() {
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		CurrentRequest.setAccessMode(AccessMode.Frontend);

		try {

			Result<AbstractNode> result = getResults(request);
			if(result != null) {

				response.setContentType("application/json; charset=utf-8");
				Writer writer = response.getWriter();

				Type type = new TypeToken<Result<AbstractNode>>() {}.getType();
				gson.toJson(result, type, writer);

				writer.flush();
				writer.close();

				response.setStatus(HttpServletResponse.SC_OK);

			} else {

				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			}

		} catch(PathException pex) {

			response.setStatus(pex.getStatus());
		}

	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}

	// ---- public static methods -----
	private Result<AbstractNode> getResults(HttpServletRequest request) throws PathException {

		RootResourceConstraint rootConstraint = createConstraintsFromRequest(request);

		return rootConstraint.getNestedResults(request);
	}

	private RootResourceConstraint createConstraintsFromRequest(HttpServletRequest request) throws PathException {

		RootResourceConstraint rootConstraint = new RootResourceConstraint();
		ResourceConstraint<AbstractNode> currentConstraint = rootConstraint;

		// fetch request path
		String path = request.getPathInfo();
		if(path != null) {

			// split request path into URI parts
			String[] pathParts = path.split("[/]+");
			for(int i=0; i<pathParts.length; i++) {

				// eliminate empty strings
				String part = pathParts[i].trim();
				if(part.length() > 0) {

					// look for matching pattern
					for(Entry<Pattern, Class> entry : constraints.entrySet()) {

						Pattern pattern = entry.getKey();
						Class type = entry.getValue();

						// try a regexp match
						Matcher matcher = pattern.matcher(pathParts[i]);
						if(matcher.matches()) {

							try {
								// instantiate resource constraint
								ResourceConstraint constraint = (ResourceConstraint)type.newInstance();
								if(constraint.acceptUriPart(part)) {

									logger.log(Level.INFO, "{0} matched, adding constraint of type {1} for part {2}", new Object[] {
										pattern.pattern(),
										type.getName(),
										part
									});

									// nest constraint and go on
									currentConstraint.setChild(constraint);
									currentConstraint = constraint;

									// first match wins, so choose priority wisely ;)
									break;
								}

							} catch(Throwable t) {

								logger.log(Level.WARNING, "Error instantiating constraint class", t);
							}
						}
					}
				}
			}

			// sort results
			String sortKey = request.getParameter(REQUEST_PARAMETER_SORT_KEY);
			if(sortKey != null) {

				String sortOrder = request.getParameter(REQUEST_PARAMETER_SORT_ORDER);
				if(sortOrder == null) {
					sortOrder = DEFAULT_VALUE_SORT_ORDER;
				}

				SortConstraint sortConstraint = new SortConstraint(sortKey, sortOrder);
				currentConstraint.setChild(sortConstraint);
				currentConstraint = sortConstraint;
			}

			// paging
			String pageSizeParameter = request.getParameter(REQUEST_PARAMETER_PAGE_SIZE);
			if(pageSizeParameter != null) {

				String pageParameter = request.getParameter(REQUEST_PARAMETER_PAGE_NUMBER);
				int pageSize = parseInt(pageSizeParameter, DEFAULT_VALUE_PAGE_SIZE);
				int page = parseInt(pageParameter, 1);

				PagingConstraint pagingConstraint = new PagingConstraint(page, pageSize);

				if(pageSize <= 0) {
					throw new IllegalPathException();
				}

				currentConstraint.setChild(pagingConstraint);
				currentConstraint = pagingConstraint;
			}
		}

		return rootConstraint;
	}

	/**
	 * Tries to parse the given String to an int value, returning
	 * defaultValue on error.
	 *
	 * @param value the source String to parse
	 * @param defaultValue the default value that will be returned when parsing fails
	 * @return the parsed value or the given default value when parsing fails
	 */
	private int parseInt(String value, int defaultValue) {

		if(value == null) {
			return defaultValue;
		}

		try {

			return Integer.parseInt(value);

		} catch(Throwable ignore) {
		}

		return defaultValue;
	}
}
