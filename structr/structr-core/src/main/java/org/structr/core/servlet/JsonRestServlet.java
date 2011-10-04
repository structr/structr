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
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
import org.structr.core.Command;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.resource.IllegalPathException;
import org.structr.core.resource.NotFoundException;
import org.structr.core.resource.PathException;
import org.structr.core.resource.adapter.GraphObjectGSONAdapter;
import org.structr.core.resource.adapter.PropertySetGSONAdapter;
import org.structr.core.resource.constraint.IdConstraint;
import org.structr.core.resource.constraint.RelationshipConstraint;
import org.structr.core.resource.constraint.PagingConstraint;
import org.structr.core.resource.constraint.RelationshipNodeConstraint;
import org.structr.core.resource.constraint.ResourceConstraint;
import org.structr.core.resource.constraint.Result;
import org.structr.core.resource.constraint.RootResourceConstraint;
import org.structr.core.resource.constraint.SortConstraint;
import org.structr.core.resource.constraint.TypeConstraint;
import org.structr.core.resource.wrapper.PropertySet;
import org.structr.core.resource.wrapper.PropertySet.PropertyFormat;

/**
 * Implements the structr REST API. The input and output format of the JSON
 * serialisation can be configured with the servlet init parameter "PropertyFormat".
 * ({@see PropertyFormat}).
 *
 * @author Christian Morgner
 */
public class JsonRestServlet extends HttpServlet {

	private static final Logger logger = Logger.getLogger(JsonRestServlet.class.getName());

	private static final String	REQUEST_PARAMETER_SORT_KEY =		"sort";
	private static final String	REQUEST_PARAMETER_SORT_ORDER =		"order";
	private static final String	REQUEST_PARAMETER_PAGE_NUMBER =		"page";
	private static final String	REQUEST_PARAMETER_PAGE_SIZE =		"pageSize";

	private static final int	DEFAULT_VALUE_PAGE_SIZE =		20;
	private static final String	DEFAULT_VALUE_SORT_ORDER =		"asc";

	private static final String	SERVLET_PARAMETER_PROPERTY_FORMAT =	"PropertyFormat";

	private Map<Pattern, Class> constraints = null;
	private Gson gson = null;

	@Override
	public void init() {

		// ----- set property format from init parameters -----
		String propertyFormatParameter = this.getInitParameter(SERVLET_PARAMETER_PROPERTY_FORMAT);
		PropertyFormat propertyFormat = PropertyFormat.NestedKeyValueType;

		if(propertyFormatParameter != null) {

			try {
				propertyFormat = PropertyFormat.valueOf(propertyFormatParameter);
				logger.log(Level.INFO, "Setting property format to {0}", propertyFormatParameter);

			} catch(Throwable t) {

				logger.log(Level.WARNING, "Cannot use property format {0}, unknown format.", propertyFormatParameter);
			}
		}

		// initialize GSON renderer
		this.constraints = new LinkedHashMap<Pattern, Class>();
		this.gson = new GsonBuilder()
			.setPrettyPrinting()
			.registerTypeAdapter(PropertySet.class, new PropertySetGSONAdapter(propertyFormat))
			.registerTypeAdapter(GraphObject.class, new GraphObjectGSONAdapter(propertyFormat))
			.create();

		// ----- initialize constraints -----
		constraints.put(Pattern.compile("[0-9]+"),	IdConstraint.class);			// this matches the ID constraint first

		// relationships
		constraints.put(Pattern.compile("out"),		RelationshipConstraint.class);		// outgoing relationship
		constraints.put(Pattern.compile("in"),		RelationshipConstraint.class);		// incoming relationship

		// start & end node
		constraints.put(Pattern.compile("start"),	RelationshipNodeConstraint.class);	// start node
		constraints.put(Pattern.compile("end"),		RelationshipNodeConstraint.class);	// end node

		// The pattern for a generic type match. This pattern should be inserted at the very end
		// of the chain because it matches everything that is a lowercase string without numbers
		constraints.put(Pattern.compile("[a-z_]+"),	TypeConstraint.class);			// any type match
	}

	@Override
	public void destroy() {
	}

	// <editor-fold defaultstate="collapsed" desc="DELETE">
	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		CurrentRequest.setAccessMode(AccessMode.Frontend);

		try {

			// obtain results and try to delete
			Result result = getResults(request);
			if(result != null) {

				final List<GraphObject> results = result.getResults();
				if(results != null && !results.isEmpty()) {

					Boolean success = (Boolean)Services.command(TransactionCommand.class).execute(new StructrTransaction() {

						@Override
						public Object execute() throws Throwable {

							boolean success = true;
							for(GraphObject obj : results) {

								success &= obj.delete();
							}

							// roll back transaction if not all deletions were successful
							if(!success) {
								// throwable will cause transaction to be rolled back
								throw new IllegalStateException("Deletion failed, roll back transaction");
							}

							return success;
						}

					});

					// return success
					if(success.booleanValue()) {
						response.setStatus(HttpServletResponse.SC_OK);
						return;
					}

				} else {
					throw new NotFoundException();
				}
			}

			// return bad request on error
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="GET">
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		CurrentRequest.setAccessMode(AccessMode.Frontend);

		try {

			double queryTimeStart = System.nanoTime();
			Result result = getResults(request);
			double queryTimeEnd = System.nanoTime();

			if(result != null) {

				DecimalFormat decimalFormat = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
				result.setQueryTime(decimalFormat.format((queryTimeEnd - queryTimeStart) / 1000000000.0));

				response.setContentType("application/json; charset=utf-8");
				Writer writer = response.getWriter();

				// GSON serialization
//				double serializationTimeStart = System.nanoTime();
				gson.toJson(result, writer);
//				double serializationTimeEnd = System.nanoTime();

				writer.flush();
				writer.close();

				response.setStatus(HttpServletResponse.SC_OK);

//				logger.log(Level.INFO, "GSON serialization took {0} seconds", decimalFormat.format((serializationTimeEnd - serializationTimeStart) / 1000000000.0));

			} else {

				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
			}

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="HEAD">
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="OPTIONS">
	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="POST">
	@Override
	protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			if(request.getPathInfo().endsWith("/node")) {

				// parse property set from json input
				final PropertySet propertySet = gson.fromJson(request.getReader(), PropertySet.class);

				// create new node
				AbstractNode newNode = (AbstractNode)Services.command(TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws Throwable {

						Command createNodeCommand = Services.command(CreateNodeCommand.class);
						return (AbstractNode)createNodeCommand.execute(new SuperUser(), propertySet.getAttributes());
					}
				});

				// build "Location" header field for response
				StringBuilder uriBuilder = new StringBuilder(40);
				// FIXME: use correct URI here! (how can we know?)
				uriBuilder.append("/api/");
				uriBuilder.append(newNode.getId());

				// set response code
				response.setHeader("Location", uriBuilder.toString());
				response.setStatus(HttpServletResponse.SC_CREATED);

			} else
			if(request.getPathInfo().endsWith("/rel")) {

				throw new IllegalPathException();

			} else {
				
				throw new IllegalPathException();
			}

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="PUT">
	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		CurrentRequest.setAccessMode(AccessMode.Frontend);

		try {
			// obtain results and try to update
			Result result = getResults(request);
			if(result != null) {

				final PropertySet propertySet = gson.fromJson(request.getReader(), PropertySet.class);
				final List<GraphObject> results = result.getResults();
				if(results != null && !results.isEmpty()) {

					// modify results in a single transaction
					Services.command(TransactionCommand.class).execute(new StructrTransaction() {

						@Override
						public Object execute() throws Throwable {

							for(GraphObject obj : results) {
								for(NodeAttribute attr : propertySet.getAttributes()) {
									obj.setProperty(attr.getKey(), attr.getValue());
								}
							}

							return null;
						}

					});
				} else {
					throw new NotFoundException();
				}

			} else {
				throw new NotFoundException();
			}

			// return bad request on error
			response.setStatus(HttpServletResponse.SC_OK);

		} catch(PathException pathException) {
			response.setStatus(pathException.getStatus());
		} catch(JsonSyntaxException jsex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(JsonParseException jpex) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} catch(Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}
	// </editor-fold>

	// <editor-fold defaultstate="collapsed" desc="TRACE">
	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

	}
	// </editor-fold>

	// ---- private methods -----
	private Result getResults(HttpServletRequest request) throws PathException {

		RootResourceConstraint rootConstraint = new RootResourceConstraint();
		ResourceConstraint currentConstraint = rootConstraint;
		PagingConstraint pagingConstraint = null;
		String sortOrder = null;
		String sortKey = null;

		// fetch request path
		String path = request.getPathInfo();
		if(path != null) {

			// parse path into constraint chain
			currentConstraint = parsePath(path, rootConstraint);

			// sort results
			sortKey = request.getParameter(REQUEST_PARAMETER_SORT_KEY);
			if(sortKey != null) {

				sortOrder = request.getParameter(REQUEST_PARAMETER_SORT_ORDER);
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

				pagingConstraint = new PagingConstraint(page, pageSize);

				if(pageSize <= 0) {
					throw new IllegalPathException();
				}

				currentConstraint.setChild(pagingConstraint);
				currentConstraint = pagingConstraint;
			}
		}

		// fetch results
		Result results = rootConstraint.getNestedResults(request);
		if(results != null) {

			// set information from paging constraint
			if(pagingConstraint != null) {

				results.setPage(pagingConstraint.getPage());
				results.setPageSize(pagingConstraint.getPageSize());
				results.setPageCount(pagingConstraint.getPageCount());
				results.setResultCount(pagingConstraint.getResultCount());

			} else {

				List<GraphObject> resultList = results.getResults();
				if(resultList != null) {
					results.setResultCount(resultList.size());
				} else {
					logger.log(Level.WARNING, "Got empty result set!");
				}
			}

			// set information from sort constraint
			results.setSortOrder(sortOrder);
			results.setSortKey(sortKey);
		}

		// finally: return results
		return results;
	}

	private ResourceConstraint parsePath(String path, ResourceConstraint rootConstraint) {

		ResourceConstraint currentConstraint = rootConstraint;

		// split request path into URI parts
		String[] pathParts = path.split("[/]+");
		for(int i=0; i<pathParts.length; i++) {

			// eliminate empty strings
			String part = pathParts[i].trim();
			if(part.length() > 0) {

				// look for matching pattern
				for(Entry<Pattern, Class> entry : constraints.entrySet()) {

					Pattern pattern = entry.getKey();
					Matcher matcher = pattern.matcher(pathParts[i]);

					if(matcher.matches()) {

						try {
							Class type = entry.getValue();

							// instantiate resource constraint
							ResourceConstraint constraint = (ResourceConstraint)type.newInstance();
							if(constraint.acceptUriPart(part)) {

								logger.log(Level.FINEST, "{0} matched, adding constraint of type {1} for part {2}", new Object[] {
									matcher.pattern(),
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

		return currentConstraint;
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
