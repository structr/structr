package org.structr.core.app;

import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.search.BooleanClause;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.graph.search.DistanceSearchAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class Query<T extends GraphObject> {

	private List<SearchAttribute> searchAttributes = new LinkedList<>();
	private Class<? extends SearchCommand> cmd     = SearchNodeCommand.class;
	private SecurityContext securityContext        = null;
	private Result<T> result                       = null;
	private PropertyKey sortKey                    = null;
	private boolean publicOnly                     = false;
	private boolean includeDeletedAndHidden        = false;
	private boolean sortDescending                 = false;
	private String offsetId                        = null;
	private int pageSize                           = Integer.MAX_VALUE;
	private int page                               = 1;
	
	
	Query(final SecurityContext securityContext, final Class<? extends SearchCommand> searchCommand) {
		this.securityContext = securityContext;
		this.cmd             = searchCommand;
	}
	
	public Result<T> getResult() throws FrameworkException {
		
		result = Services.command(securityContext, cmd).execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDescending, pageSize, page, offsetId);

		return result;
	}
	
	public List<T> getAsList() throws FrameworkException {
		return getResult().getResults();
	}
	
	public T getFirst() throws FrameworkException {
		
		if (result == null) {
			getResult();
		}

		if (result.isEmpty()) {
			
			return null;
		}
		
		return result.get(0);
	}
	
	public int resultCount() throws FrameworkException {

		if (result == null) {
			getResult();
		}
		
		return result.size();
	}

	// ----- builder methods -----
	public Query<T> sort(final PropertyKey key) {
		return sortAscending(key);
	}
	
	public Query<T> sortAscending(final PropertyKey key) {
		
		this.sortDescending = false;
		this.sortKey = key;
		
		return this;
	}
	
	public Query<T> sortDescending(final PropertyKey key) {

		this.sortDescending = true;
		this.sortKey = key;
		
		return this;
	}
	
	public Query<T> order(final boolean descending) {
		
		this.sortDescending = descending;
		return this;
	}
	
	public Query<T> pageSize(final int pageSize) {
		this.pageSize = pageSize;
		return this;
	}
	
	public Query<T> page(final int page) {
		this.page = page;
		return this;
	}
	
	public Query<T> publicOnly() {
		this.publicOnly = true;
		return this;
	}
	
	public Query<T> includeDeletedAndHidden() {
		this.includeDeletedAndHidden = true;
		return this;
	}
	
	public Query<T> offsetId(final String offsetId) {
		this.offsetId = offsetId;
		return this;
	}
	
	public Query<T> uuid(final String uuid) {
		searchAttributes.add(Search.andExactUuid(uuid));
		return this;
	}
	
	public Query<T> type(final Class<T> type) {
		searchAttributes.add(Search.andExactType(type));
		return this;
	}
	
	public Query<T> name(final String name) {
		searchAttributes.add(Search.andExactName(name));
		return this;
	}
	
	public Query<T> location(final String street, final String postalCode, final String city, final String country, final double distance) {
		return location(street, null, postalCode, city, null, country, distance);
	}
	
	public Query<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance) {
		return location(street, null, postalCode, city, state, country, distance);
	}
	
	public Query<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance) {
		searchAttributes.add(new DistanceSearchAttribute(street, house, postalCode, city, state, country, distance, BooleanClause.Occur.MUST));
		return this;
	}
	
	public <P> Query<T> and(final PropertyKey<P> key, P value) {
		searchAttributes.add(Search.andExactProperty(securityContext, key, value));
		return this;
	}
	
	public <P> Query<T> or(final PropertyKey<P> key, P value) {
		searchAttributes.add(Search.orExactProperty(securityContext, key, value));
		return this;
	}
}
