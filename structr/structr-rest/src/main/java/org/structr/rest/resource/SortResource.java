/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.resource;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.common.AbstractGraphObjectComparator;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidSortKey;
import org.structr.core.GraphObject;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.servlet.JsonRestServlet;

/**
 *
 * @author Christian Morgner
 */
public class SortResource extends WrappingResource {

	private static final Logger logger = Logger.getLogger(SortResource.class.getName());

	private String sortOrder = null;
	private String sortKey = null;
	
	public SortResource(SecurityContext securityContext, String sortKey, String sortOrder) {
		this.securityContext = securityContext;
		this.sortKey = sortKey;
		this.sortOrder = sortOrder;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.sortKey = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_KEY);
		this.sortOrder = request.getParameter(JsonRestServlet.REQUEST_PARAMETER_SORT_ORDER);

		return sortKey != null;
	}
	
	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {

		if(wrappedResource != null) {
			
			List<? extends GraphObject> results = wrappedResource.doGet();

			try {
				Collections.sort(results, new AbstractGraphObjectComparator(sortKey, sortOrder));
				
			} catch(Throwable t) {
				
				throw new FrameworkException("base", new InvalidSortKey(sortKey));
			}

			return results;
		}

		throw new IllegalPathException();
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return super.tryCombineWith(next);
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public String getSortKey() {
		return sortKey;
	}
	
	@Override
	public void postProcessResultSet(Result result) {
		result.setSortOrder(sortOrder);
		result.setSortKey(sortKey);
	}
}
