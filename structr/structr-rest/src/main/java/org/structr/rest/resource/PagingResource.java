/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.rest.resource;

import java.util.List;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 * Implements paging.
 *
 * @author Christian Morgner
 */
public class PagingResource extends WrappingResource {

	private static final Logger logger = Logger.getLogger(PagingResource.class.getName());

	private int resultCount = 0;
	private int pageSize = 0;
	private int page = 0;

	public PagingResource(SecurityContext securityContext, int page, int pageSize) {
		this.securityContext = securityContext;
		this.page = page;
		this.pageSize = pageSize;
	}

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {
		return false;
	}

	@Override
	public List<? extends GraphObject> doGet() throws FrameworkException {

		List<? extends GraphObject> results = wrappedResource.doGet();
		resultCount = results.size();

		int fromIndex = Math.min(resultCount, Math.max(0, (getPage()-1) * getPageSize()));
		int toIndex = Math.min(resultCount, getPage()*getPageSize());

		return results.subList(fromIndex, toIndex);
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {
		return super.tryCombineWith(next);
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getResultCount() {
		return resultCount;
	}

	public int getPageCount() {
		return (int)Math.rint(Math.ceil((double)getResultCount() / (double)getPageSize()));
	}
	
	@Override
	public void postProcessResultSet(Result result) {
		
		result.setResultCount(getResultCount());
		result.setPage(getPage());
		result.setPageSize(getPageSize());
		result.setPageCount(getPageCount());
		
		// add sorting parameters
		if(wrappedResource != null && wrappedResource instanceof SortResource) {
			((SortResource)wrappedResource).postProcessResultSet(result);
		}
		
		super.postProcessResultSet(result);
	}

        @Override
        public String getResourceSignature() {
		
		String uriPart    = getUriPart();
		StringBuilder uri = new StringBuilder();

		if (uriPart.contains("/")) {

			String[] parts = StringUtils.split(uriPart, "/");

			for (String subPart : parts) {

				if (!subPart.matches("[a-zA-Z0-9]{32}")) {

					uri.append(subPart);
					uri.append("/");

				}

			}

			return uri.toString();

		} else {

			return uriPart;

		}
	}
}
