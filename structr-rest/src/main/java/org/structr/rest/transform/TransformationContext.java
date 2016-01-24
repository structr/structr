package org.structr.rest.transform;

import org.structr.common.SecurityContext;
import org.structr.core.property.PropertyKey;

/**
 *
 */
public class TransformationContext {

	private SecurityContext securityContext = null;
	private PropertyKey sortKey             = null;
	private boolean sortDescending          = false;
	private String offsetId                 = null;
	private int rawResultCount              = 0;
	private int pageSize                    = 0;
	private int page                        = 0;

	public TransformationContext(final SecurityContext securityContext, final PropertyKey sortKey, final int pageSize, final int page, final boolean sortDescending, final String offsetId) {

		this.securityContext = securityContext;
		this.sortKey         = sortKey;
		this.sortDescending  = sortDescending;
		this.offsetId        = offsetId;
		this.pageSize        = pageSize;
		this.page            = page;
	}

	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	public PropertyKey getSortKey() {
		return sortKey;
	}

	public boolean isSortDescending() {
		return sortDescending;
	}

	public String getOffsetId() {
		return offsetId;
	}

	public int getPageSize() {
		return pageSize;
	}

	public int getPage() {
		return page;
	}

	public int getRawResultCount() {
		return rawResultCount;
	}

	public void setRawResultCount(final int rawResultCount) {
		this.rawResultCount = rawResultCount;
	}
}
