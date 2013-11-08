package org.structr.core.app;

import java.util.List;
import java.util.Map;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.graph.search.DistanceSearchAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */
public class StructrQuery<T extends GraphObject> implements Query<T> {

	private SearchAttributeGroup rootGroup     = new SearchAttributeGroup(Occur.MUST);
	private SearchAttributeGroup currentGroup  = rootGroup;
	private Class<? extends SearchCommand> cmd = SearchNodeCommand.class;
	private SecurityContext securityContext    = null;
	private Result<T> result                   = null;
	private PropertyKey sortKey                = null;
	private boolean publicOnly                 = false;
	private boolean includeDeletedAndHidden    = false;
	private boolean sortDescending             = false;
	private String offsetId                    = null;
	private int pageSize                       = Integer.MAX_VALUE;
	private int page                           = 1;
	
	
	StructrQuery(final SecurityContext securityContext, final Class<? extends SearchCommand> searchCommand) {
		this.securityContext = securityContext;
		this.cmd             = searchCommand;
	}
	
	@Override
	public Result<T> getResult() throws FrameworkException {
		
		result = Services.command(securityContext, cmd).execute(includeDeletedAndHidden, publicOnly, rootGroup.getSearchAttributes(), sortKey, sortDescending, pageSize, page, offsetId);

		return result;
	}
	
	@Override
	public List<T> getAsList() throws FrameworkException {
		return getResult().getResults();
	}
	
	@Override
	public T getFirst() throws FrameworkException {
		
		if (result == null) {
			getResult();
		}

		if (result.isEmpty()) {
			
			return null;
		}
		
		return result.get(0);
	}
	
	@Override
	public int resultCount() throws FrameworkException {

		if (result == null) {
			getResult();
		}
		
		return result.size();
	}

	// ----- builder methods -----
	@Override
	public Query<T> sort(final PropertyKey key) {
		return sortAscending(key);
	}
	
	@Override
	public Query<T> sortAscending(final PropertyKey key) {
		
		this.sortDescending = false;
		this.sortKey = key;
		
		return this;
	}
	
	@Override
	public Query<T> sortDescending(final PropertyKey key) {

		this.sortDescending = true;
		this.sortKey = key;
		
		return this;
	}
	
	@Override
	public Query<T> order(final boolean descending) {
		
		this.sortDescending = descending;
		return this;
	}
	
	@Override
	public Query<T> pageSize(final int pageSize) {
		this.pageSize = pageSize;
		return this;
	}
	
	@Override
	public Query<T> page(final int page) {
		this.page = page;
		return this;
	}
	
	@Override
	public Query<T> publicOnly() {
		this.publicOnly = true;
		return this;
	}
	
	@Override
	public Query<T> publicOnly(final boolean publicOnly) {
		this.publicOnly = publicOnly;
		return this;
	}
	
	@Override
	public Query<T> includeDeletedAndHidden() {
		this.includeDeletedAndHidden = true;
		return this;
	}
	
	@Override
	public Query<T> includeDeletedAndHidden(final boolean includeDeletedAndHidden) {
		this.includeDeletedAndHidden = includeDeletedAndHidden;
		return this;
	}
	
	@Override
	public Query<T> offsetId(final String offsetId) {
		this.offsetId = offsetId;
		return this;
	}
	
	@Override
	public Query<T> uuid(final String uuid) {
		currentGroup.getSearchAttributes().add(Search.andExactUuid(uuid));
		return this;
	}
	
	@Override
	public Query<T> type(final Class<T> type) {
		currentGroup.getSearchAttributes().add(Search.andExactType(type));
		return this;
	}
	
	@Override
	public Query<T> types(final Class<T> type) {
		currentGroup.getSearchAttributes().add(Search.andExactTypeAndSubtypes(type));
		return this;
	}
	
	@Override
	public Query<T> types(final Class<T> type, final boolean inexact) {
		currentGroup.getSearchAttributes().add(Search.andTypeAndSubtypes(type, false));
		return this;
	}

	@Override
	public Query<T> andName(final String name) {
		currentGroup.getSearchAttributes().add(Search.andExactName(name));
		return this;
	}
	
	@Override
	public Query<T> orName(final String name) {
		currentGroup.getSearchAttributes().add(Search.orExactName(name));
		return this;
	}
	
	@Override
	public Query<T> location(final String street, final String postalCode, final String city, final String country, final double distance) {
		return location(street, null, postalCode, city, null, country, distance);
	}
	
	@Override
	public Query<T> location(final String street, final String postalCode, final String city, final String state, final String country, final double distance) {
		return location(street, null, postalCode, city, state, country, distance);
	}
	
	@Override
	public Query<T> location(final String street, final String house, final String postalCode, final String city, final String state, final String country, final double distance) {
		currentGroup.getSearchAttributes().add(new DistanceSearchAttribute(street, house, postalCode, city, state, country, distance, BooleanClause.Occur.MUST));
		return this;
	}
	
	@Override
	public <P> Query<T> and(final PropertyKey<P> key, final P value) {
		currentGroup.getSearchAttributes().add(Search.andExactProperty(securityContext, key, value));
		return this;
	}
	
	@Override
	public <P> Query<T> and(final PropertyKey<P> key, final P value, final boolean inexact) {
		if (inexact) {
			currentGroup.getSearchAttributes().add(Search.andProperty(securityContext, key, value));
		} else {
			currentGroup.getSearchAttributes().add(Search.andExactProperty(securityContext, key, value));
		}
		return this;
	}

	@Override
	public <P> Query<T> and(final PropertyMap attributes) {
		
		for (Map.Entry<PropertyKey, Object> entry : attributes.entrySet()) {
			PropertyKey key = entry.getKey();
			Object value = entry.getValue();
			currentGroup.getSearchAttributes().add(Search.andExactProperty(securityContext, key, value));
		}
		
		
		return this;
	}

	@Override
	public Query<T> and() {
		
		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, Occur.MUST);
		currentGroup.getSearchAttributes().add(group);
		currentGroup = group;
		
		return this;
	}
	
	@Override
	public <P> Query<T> or(final PropertyKey<P> key, P value) {
		currentGroup.getSearchAttributes().add(Search.orExactProperty(securityContext, key, value));
		return this;
	}

	@Override
	public <P> Query<T> or(final PropertyMap attributes) {
		
		for (Map.Entry<PropertyKey, Object> entry : attributes.entrySet()) {
			PropertyKey key = entry.getKey();
			Object value = entry.getValue();
			currentGroup.getSearchAttributes().add(Search.orExactProperty(securityContext, key, value));
		}
		
		
		return this;
	}
	
	@Override
	public Query<T> or() {
		
		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, Occur.SHOULD);
		currentGroup.getSearchAttributes().add(group);
		currentGroup = group;
		
		return this;
	}
	
	@Override
	public Query<T> not() {
		
		// create nested group that the user can add to
		final SearchAttributeGroup group = new SearchAttributeGroup(currentGroup, Occur.MUST_NOT);
		currentGroup.getSearchAttributes().add(group);
		currentGroup = group;
		
		return this;
	}
	
	@Override
	public Query<T> parent() {
		
		// one level up
		SearchAttributeGroup parent = currentGroup.getParent();
		if (parent != null) {
			
			currentGroup = parent;
		}

		return this;
	}

	@Override
	public Query<T> attributes(final List<SearchAttribute> attributes) {

		currentGroup.getSearchAttributes().addAll(attributes);
		return this;
	}
}
