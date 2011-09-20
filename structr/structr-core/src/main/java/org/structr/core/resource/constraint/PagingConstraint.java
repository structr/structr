/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.resource.constraint;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.structr.core.entity.AbstractNode;
import org.structr.core.resource.PathException;

/**
 *
 * @author Christian Morgner
 */
public class PagingConstraint extends ResourceConstraint<AbstractNode> {

	private static final Logger logger = Logger.getLogger(PagingConstraint.class.getName());

	private int pageSize = 0;
	private int listSize = 0;
	private int page = 0;

	public PagingConstraint(int page, int pageSize) {
		this.page = page;
		this.pageSize = pageSize;
	}

	@Override
	public boolean acceptUriPart(String part) {
		return false;
	}

	@Override
	public Result<AbstractNode> processParentResult(Result<AbstractNode> result, HttpServletRequest request) throws PathException {

		/*
		 * page 1: 0 -> pageSize-1
		 * page 2: pageSize -> (2*pageSize)-1
		 * page 3: (2*pageSize) -> (3*pageSize)-1
		 * page n: ((n-1) * pageSize) -> (n * pageSize) - 1
		 */

		List<AbstractNode> list = result.getResults();

		listSize = list.size();
		int fromIndex = Math.min(listSize, Math.max(0, (getPage()-1) * getPageSize()));
		int toIndex = Math.min(listSize, getPage()*getPageSize());

		logger.log(Level.FINEST, "returning results from {0} to {1}, page {2}, pageSize {3}", new Object[] { fromIndex, toIndex-1, getPage(), getPageSize()} );

		return new Result(list.subList(fromIndex, toIndex));
	}

	public int getListSize() {
		return listSize;
	}

	@Override
	public boolean supportsMethod(String method) {
		if("GET".equals(method)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean supportsNesting() {
		return true;
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
}
