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
package org.structr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the result of a query operation.
 *
 *
 *
 */
public class Result<T extends GraphObject> {

	public static final Result EMPTY_RESULT = new Result(Collections.EMPTY_LIST, 0, false, false);

	private boolean isCollection      = false;
	private boolean isPrimitiveArray  = false;
	private boolean hasPartialContent = false;
	private String propertyView       = null;
	private List<T> results           = null;

	private String searchString       = null;
	private String queryTime          = null;
	private String sortOrder          = null;
	private String sortKey            = null;

	private Integer resultCount       = null;
	private Integer pageCount         = null;
	private Integer pageSize          = null;
	private Integer page              = null;

	private GraphObject metaData      = null;

	public Result(final List<T> listResult, final Integer rawResultCount, final boolean isCollection, final boolean isPrimitiveArray) {

		this.isCollection     = isCollection;
		this.isPrimitiveArray = isPrimitiveArray;
		this.results          = listResult;
		this.resultCount      = (rawResultCount != null ? rawResultCount : (results != null ? results.size() : 0));
	}

	public Result(T singleResult, final boolean isPrimitiveArray) {

		this.isCollection     = false;
		this.isPrimitiveArray = isPrimitiveArray;
		this.results          = new ArrayList<>();
		this.resultCount      = singleResult != null ? 1 : 0;

		if (singleResult != null) {

			// add result
			results.add(singleResult);
		}
	}

	@Override
	public String toString() {
		return "Result(" + (results != null ? results.size() : 0) + ")";
	}

	public T get(final int i) {
		return results.get(i);
	}

	public boolean isEmpty() {
		return results == null || results.isEmpty();
	}

	public List<T> getResults() {
		return results;
	}

	public void setQueryTime(final String queryTime) {
		this.queryTime = queryTime;
	}

	public String getQueryTime() {
		return queryTime;
	}

	public String getSearchString() {
		return searchString;
	}

	public void setSearchString(final String searchString) {
		this.searchString = searchString;
	}

	public String getSortKey() {
		return sortKey;
	}

	public void setSortKey(final String sortKey) {
		this.sortKey = sortKey;
	}

	public Integer getRawResultCount() {

		if (resultCount != null) {
			return resultCount;
		}

		return size();
	}

	public void setRawResultCount(final Integer resultCount) {
		this.resultCount = resultCount;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public void setPageCount(final Integer pageCount) {
		this.pageCount = pageCount;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(final Integer page) {
		this.page = page;
	}

	public String getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(final String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(final Integer pageSize) {
		this.pageSize = pageSize;
	}

	public String getPropertyView() {
		return propertyView;
	}

	public void setPropertyView(final String propertyView) {
		this.propertyView = propertyView;
	}

	public boolean isCollection() {
		return isCollection;
	}

	public boolean isPrimitiveArray() {
		return isPrimitiveArray;
	}

	public void setIsPrimitiveArray(final boolean isPrimitiveArray) {
		this.isPrimitiveArray = isPrimitiveArray;
	}

	public void setIsCollection(final boolean isCollection) {
		this.isCollection = isCollection;
	}

	public int size() {
		return !isEmpty() ? results.size() : 0;
	}

	public void setHasPartialContent(boolean hasPartialContent) {
		this.hasPartialContent = hasPartialContent;
	}

	public boolean hasPartialContent() {
		return hasPartialContent;
	}

	public GraphObject getMetaData() {
		return metaData;
	}

	public void setMetaData(GraphObject metaData) {
		this.metaData = metaData;
	}
}
