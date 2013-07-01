/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.graph.NodeService.RelationshipIndex;
import org.structr.core.graph.search.RangeSearchAttribute;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.PropertySearchAttribute;
import org.structr.core.graph.search.SearchCommand;

/**
 * Abstract base class for all property types.
 *
 * @author Christian Morgner
 */
public abstract class Property<T> implements PropertyKey<T> {

	private static final Logger logger             = Logger.getLogger(Property.class.getName());
	private static final Pattern rangeQueryPattern = Pattern.compile("\\[(.+) TO (.+)\\]");
	
	protected List<PropertyValidator<T>> validators        = new LinkedList<PropertyValidator<T>>();
	protected Class<? extends GraphObject> declaringClass  = null;
	protected T defaultValue                               = null;
	protected boolean readOnly                             = false;
	protected boolean writeOnce                            = false;
	protected boolean unvalidated                          = false;
	protected boolean indexed                              = false;
	protected boolean indexedPassively                     = false;
	protected String dbName                                = null;
	protected String jsonName                              = null;

	private boolean requiresSynchronization                = false;
	
	protected Set<RelationshipIndex> relationshipIndices   = new LinkedHashSet<RelationshipIndex>();
	protected Set<NodeIndex> nodeIndices                   = new LinkedHashSet<NodeIndex>();

	protected Property(String name) {
		this(name, name);
	}
	
	protected Property(String jsonName, String dbName) {
		this(jsonName, dbName, null);
	}
	
	protected Property(String jsonName, String dbName, T defaultValue) {
		this.defaultValue = defaultValue;
		this.jsonName = jsonName;
		this.dbName = dbName;
	}
	
	public abstract Object fixDatabaseProperty(Object value);
	
	/**
	 * Use this method to mark a property as being unvalidated. This
	 * method will cause no callbacks to be executed when only
	 * unvalidated properties are modified.
	 * 
	 * @return  the Property to satisfy the builder pattern
	 */
	public Property<T> unvalidated() {
		this.unvalidated = true;
		return this;
	}
	
	/**
	 * Use this method to mark a property as being read-only.
	 * 
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> readOnly() {
		this.readOnly = true;
		return this;
	}
	
	/**
	 * Use this method to mark a property as being write-once.
	 * 
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> writeOnce() {
		this.writeOnce = true;
		return this;
	}
	
	/**
	 * Use this method to mark a property for indexing. This
	 * method registers the property in both the keyword and
	 * the fulltext index. To select the appropriate index
	 * for yourself, use the other indexed() methods.
	 * 
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> indexed() {
		this.indexed = true;
				
		nodeIndices.add(NodeIndex.fulltext);
		nodeIndices.add(NodeIndex.keyword);
		
		relationshipIndices.add(RelationshipIndex.rel_fulltext);
		relationshipIndices.add(RelationshipIndex.rel_keyword);
		
		return this;
	}
	
	/**
	 * Use this method to mark a property for indexing 
	 * in the given index.
	 * 
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> indexed(NodeIndex nodeIndex) {
		
		this.indexed = true;
		nodeIndices.add(nodeIndex);
		
		return this;
	}
	
	/**
	 * Use this method to mark a property for indexing 
	 * in the given index.
	 * 
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> indexed(RelationshipIndex relIndex) {
		
		this.indexed = true;
		relationshipIndices.add(relIndex);
		
		return this;
	}
	
	/**
	 * Use this method to indicate that a property key can change its value
	 * without setProperty() being called directly on it. This method causes
	 * the given property to be indexed at the end of a transaction instead
	 * of immediately on setProperty(). This method registers the property
	 * in both the keyword and the fulltext index. To select the appropriate
	 * index for yourself, use the other indexed() methods.
	 * 
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> passivelyIndexed() {
		this.indexedPassively = true;
		this.indexed = true;
				
		nodeIndices.add(NodeIndex.fulltext);
		nodeIndices.add(NodeIndex.keyword);
		
		relationshipIndices.add(RelationshipIndex.rel_fulltext);
		relationshipIndices.add(RelationshipIndex.rel_keyword);
		
		return this;
	}
	
	/**
	 * Use this method to indicate that a property key can change its value
	 * without setProperty() being called directly on it. This method causes
	 * the given property to be indexed at the end of a transaction instead
	 * of immediately on setProperty().
	 * 
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> passivelyIndexed(NodeIndex nodeIndex) {
		this.indexedPassively = true;
		this.indexed = true;
		nodeIndices.add(nodeIndex);
		return this;
	}
	
	/**
	 * Use this method to indicate that a property key can change its value
	 * without setProperty() being called directly on it. This method causes
	 * the given property to be indexed at the end of a transaction instead
	 * of immediately on setProperty().
	 * 
	 * @return the Property to satisfy the builder pattern
	 */
	public Property<T> passivelyIndexed(RelationshipIndex relIndex) {
		this.indexedPassively = true;
		this.indexed = true;
		relationshipIndices.add(relIndex);
		return this;
	}

	@Override
	public void addValidator(PropertyValidator<T> validator) {
		
		validators.add(validator);
		
		// fetch synchronization requirement from validator
		if (validator.requiresSynchronization()) {
			this.requiresSynchronization = true;
		}
	}
	
	public Property<T> validator(PropertyValidator<T> validator) {
		addValidator(validator);
		return this;
	}

	@Override
	public List<PropertyValidator<T>> getValidators() {
		return validators;
	}

	@Override
	public boolean requiresSynchronization() {
		return requiresSynchronization;
	}
	
	@Override
	public String getSynchronizationKey() {
		return dbName;
	}
	
	@Override
	public void setDeclaringClass(Class<? extends GraphObject> declaringClass) {
		this.declaringClass = declaringClass;
	}
	
	@Override
	public void registrationCallback(Class type) {
	}
	
	@Override
	public Class<? extends GraphObject> getDeclaringClass() {
		return declaringClass;
	}

	
	@Override
	public String toString() {
		return "(".concat(jsonName()).concat("|").concat(dbName()).concat(")");
	}
	
	@Override
	public String dbName() {
		return dbName;
	}
	
	@Override
	public String jsonName() {
		return jsonName;
	}
	
	@Override
	public T defaultValue() {
		return defaultValue;
	}
	
	@Override
	public int hashCode() {
		
		// make hashCode funtion work for subtypes that override jsonName() etc. as well
		if (dbName() != null && jsonName() != null) {
			return (dbName().hashCode() * 31) + jsonName().hashCode();
		}
		
		if (dbName() != null) {
			return dbName().hashCode();
		}
		
		if (jsonName() != null) {
			return jsonName().hashCode();
		}
		
		// TODO: check if it's ok if null key is not unique
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o instanceof PropertyKey) {
		
			return o.hashCode() == hashCode();
		}
		
		return false;
	}

	@Override
	public boolean isUnvalidated() {
		return unvalidated;
	}

	@Override
	public boolean isReadOnlyProperty() {
		return readOnly;
	}
	
	@Override
	public boolean isWriteOnceProperty() {
		return writeOnce;
	}
	
	@Override
	public boolean isIndexedProperty() {
		return indexed;
	}
	
	@Override
	public boolean isPassivelyIndexedProperty() {
		return indexedPassively;
	}
	
	@Override
	public void index(GraphObject entity, Object value) {

		if (entity instanceof AbstractNode) {

			NodeService nodeService = Services.getService(NodeService.class);
			AbstractNode node       = (AbstractNode)entity;

			for (NodeIndex indexName : nodeIndices()) {

				Index<Node> index = nodeService.getNodeIndex(indexName);
				if (index != null) {

					synchronized (index) {

						index.remove(node.getNode(), dbName);
						
						if (value != null) {
							
							index.add(node.getNode(), dbName, value);
							
						} else {
							
							index.add(node.getNode(), dbName, SearchCommand.IMPROBABLE_SEARCH_VALUE);
						}
					}
				}
			}
			
		} else {
			
			NodeService nodeService  = Services.getService(NodeService.class);
			AbstractRelationship rel = (AbstractRelationship)entity;

			for (RelationshipIndex indexName : relationshipIndices()) {

				Index<Relationship> index = nodeService.getRelationshipIndex(indexName);
				if (index != null) {

					synchronized (index) {

						index.remove(rel.getRelationship(), dbName);
							
						if (value != null) {

							index.add(rel.getRelationship(), dbName, value);
							
						} else {
							
							index.add(rel.getRelationship(), dbName, SearchCommand.IMPROBABLE_SEARCH_VALUE);
						}
					}
				}
			}
		}
		
	}
	
	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, T searchValue, boolean exactMatch) {
		return new PropertySearchAttribute(this, searchValue, occur, exactMatch);
	}
		
	@Override
	public List<SearchAttribute> extractSearchableAttribute(SecurityContext securityContext, HttpServletRequest request, boolean looseSearch) throws FrameworkException {
					
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
		String searchValue                     = request.getParameter(jsonName());
		if (searchValue != null) {

			if (looseSearch) {

				// no quotes allowed in loose search queries!
				searchValue = removeQuotes(searchValue);

				searchAttributes.add(new PropertySearchAttribute(this, searchValue, Occur.MUST, false));

			} else {

				SearchAttribute attr = determineSearchType(securityContext, this, searchValue);
				if (attr != null) {

					searchAttributes.add(attr);
				}
			}
		}
		
		return searchAttributes;
	}
	
	@Override
	public T extractSearchableAttribute(SecurityContext securityContext, String requestParameter) throws FrameworkException {

		PropertyConverter inputConverter = inputConverter(securityContext);
		Object convertedSearchValue      = requestParameter;

		if (inputConverter != null) {

			convertedSearchValue = inputConverter.convert(convertedSearchValue);
		}

		return (T)convertedSearchValue;
	}

	public Set<NodeIndex> nodeIndices() {
		return nodeIndices;
	}
	
	public Set<RelationshipIndex> relationshipIndices() {
		return relationshipIndices;
	}
	
	// ----- protected methods -----
	protected final String removeQuotes(final String searchValue) {
		String resultStr = searchValue;

		if (resultStr.contains("\"")) {
			resultStr = resultStr.replaceAll("[\"]+", "");
		}

		if (resultStr.contains("'")) {
			resultStr = resultStr.replaceAll("[']+", "");
		}

		return resultStr;
	}

	protected final SearchAttribute determineSearchType(final SecurityContext securityContext, final PropertyKey key, final String requestParameter) throws FrameworkException {

		if (StringUtils.startsWith(requestParameter, "[") && StringUtils.endsWith(requestParameter, "]")) {

			// check for existance of range query string
			Matcher matcher = rangeQueryPattern.matcher(requestParameter);
			if (matcher.matches()) {

				if (matcher.groupCount() == 2) {

					String rangeStart = matcher.group(1);
					String rangeEnd = matcher.group(2);

					PropertyConverter inputConverter = key.inputConverter(securityContext);
					Object rangeStartConverted = rangeStart;
					Object rangeEndConverted = rangeEnd;

					if (inputConverter != null) {

						rangeStartConverted = inputConverter.convert(rangeStartConverted);
						rangeEndConverted = inputConverter.convert(rangeEndConverted);
					}

					return new RangeSearchAttribute(key, rangeStartConverted, rangeEndConverted, Occur.SHOULD);
				}

				return null;
			}
		}

		if (requestParameter.contains(",") && requestParameter.contains(";")) {
			throw new FrameworkException(422, "Mixing of AND and OR not allowed in request parameters");
		}

		if (requestParameter.contains(";")) {

			return Search.orExactProperty(securityContext, key, extractSearchableAttribute(securityContext, requestParameter));
			
		} else {
			
			return Search.andExactProperty(securityContext, key, extractSearchableAttribute(securityContext, requestParameter));
		}
	}
}
