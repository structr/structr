/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.resource.constraint;

import java.util.LinkedList;
import java.util.List;
import org.structr.core.entity.AbstractNode;

/**
 * Encapsulates the result of a query operation.
 *
 * @author Christian Morgner
 */
public class Result<T extends AbstractNode> {

	private List<T> results = null;

	private String searchString = null;
	private String queryTime = null;
	private String sortOrder = null;
	private String sortKey = null;

	private Integer resultCount = null;
	private Integer pageCount = null;
	private Integer pageSize = null;
	private Integer page = null;

	public Result(T singleResult) {
		this.results = new LinkedList<T>();
		this.results.add(singleResult);
	}

	public Result(List<T> listResult) {
		this.results = listResult;
	}

	public List<T> getResults() {
		return results;
	}

	public void setQueryTime(String queryTime) {
		this.queryTime = queryTime;
	}

	public String getQueryTime() {
		return queryTime;
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}

	public String getSortKey() {
		return sortKey;
	}

	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	public Integer getResultCount() {
		return resultCount;
	}

	public void setResultCount(Integer resultCount) {
		this.resultCount = resultCount;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(Integer pageCount) {
		this.pageCount = pageCount;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
}
