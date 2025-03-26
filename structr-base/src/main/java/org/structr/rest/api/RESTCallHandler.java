/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.rest.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.NotFoundException;
import org.structr.api.search.SortOrder;
import org.structr.api.util.Iterables;
import org.structr.api.util.ResultStream;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.RequestKeywords;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.CaseHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PropertyContainerTraitDefinition;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalMethodException;
import org.structr.rest.exception.IllegalPathException;

import java.util.*;

import static java.lang.Boolean.parseBoolean;

/**
 *
 */
public abstract class RESTCallHandler {

	private static final Logger logger = LoggerFactory.getLogger(RESTCallHandler.class.getName());

	private GraphObject cachedEntity = null;
	protected String requestedView  = null;
	protected RESTCall call         = null;

	public RESTCallHandler(final RESTCall call) {
		this.call = call;
	}

	public abstract Set<String> getAllowedHttpMethodsForOptionsCall();
	public abstract boolean isCollection();
	public abstract String getTypeName(final SecurityContext securityContext) throws FrameworkException;

	public void setRequestedView(final String view) {
		this.requestedView = view;
	}

	public String getRequestedView() {
		return requestedView;
	}

	public String getURL() {
		return call.getURL();
	}

	public String getResourceSignature() {

		String signature = call.getResourceSignature();

		// remove leading slash from resource access grant
		if (signature.startsWith("/")) {
			signature = signature.substring(1);
		}

		// append requested view to resource signature
		if (!isDefaultView()) {

			signature += "/_" + CaseHelper.toUpperCamelCase(requestedView);
		}

		return signature;
	}

	/**
	 * Default implementation of the HTTP GET method that returns 405 Method Not Allowed. Override this method
	 * to provide an actual implementation.
	 *
	 * @param securityContext
	 * @param sortOrder
	 * @param pageSize
	 * @param page
	 * @return
	 * @throws org.structr.common.error.FrameworkException
	 */
	public <T> ResultStream<T> doGet(final SecurityContext securityContext, final SortOrder sortOrder, final int pageSize, final int page) throws FrameworkException {
		throw new IllegalMethodException("GET not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
	}

	/**
	 * Default implementation of the HTTP HEAD method that returns 405 Method Not Allowed. Override this method
	 * to provide an actual implementation.
	 *
	 * @return
	 * @throws org.structr.common.error.FrameworkException
	 */
	public RestMethodResult doHead() throws FrameworkException {
		throw new IllegalMethodException("HEAD not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
	}

	/**
	 * Default implementation of the HTTP PATCH method that returns 405 Method Not Allowed. Override this method
	 * to provide an actual implementation.
	 *
	 * @param securityContext
	 * @param propertySet
	 * @return
	 * @throws org.structr.common.error.FrameworkException
	 */
	public RestMethodResult doPatch(final SecurityContext securityContext, final List<Map<String, Object>> propertySet) throws FrameworkException {
		throw new IllegalMethodException("PATCH not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
	}

	/**
	 * Default implementation of the HTTP DELETE method that returns 405 Method Not Allowed. Override this method
	 * to provide an actual implementation.
	 *
	 * @param securityContext
	 * @return
	 * @throws org.structr.common.error.FrameworkException
	 */
	public RestMethodResult doDelete(final SecurityContext securityContext) throws FrameworkException {
		throw new IllegalMethodException("DELETE not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
	}

	/**
	 * Default implementation of the HTTP PUT method that returns 405 Method Not Allowed. Override this method
	 * to provide an actual implementation.
	 *
	 * @param securityContext
	 * @param propertySet
	 * @return
	 * @throws org.structr.common.error.FrameworkException
	 */
	public RestMethodResult doPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("PUT not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
	}

	/**
	 * Default implementation of the HTTP POST method that returns 405 Method Not Allowed. Override this method
	 * to provide an actual implementation.
	 *
	 * @param securityContext
	 * @param propertySet
	 * @return
	 * @throws org.structr.common.error.FrameworkException
	 */
	public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {
		throw new IllegalMethodException("POST not allowed on " + getURL(), getAllowedHttpMethodsForOptionsCall());
	}

	public boolean isPrimitiveArray() {
		return false;
	}

	/**
	 * Override this method in your resource implementation and return false
	 * to prevent the creation of an encosing transaction context in your
	 * doPost() method. Default: true.
	 *
	 * @return whether to create transaction around the doPost() method
	 */
	public boolean createPostTransaction() {
		return true;
	}

	public String getEntityClassOrDefault(final SecurityContext securityContext) {

		try {

			final String typeName = getTypeName(securityContext);
			if (typeName != null) {

				return typeName;
			}

		} catch (FrameworkException fex) {
			// what do?
			fex.printStackTrace();
		}

		return StructrTraits.NODE_INTERFACE;
	}

	public org.structr.core.graph.NodeInterface createNode(final SecurityContext securityContext, final String typeName, final Map<String, Object> propertySet) throws FrameworkException {

		final Traits traits = Traits.of(typeName);
		if (traits != null) {

			// experimental: instruct deserialization strategies to set properties on related nodes
			securityContext.setAttribute("setNestedProperties", true);

			final App app                = StructrApp.getInstance(securityContext);
			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, typeName, propertySet);

			return app.create(typeName, properties);
		}

		throw new NotFoundException("Type " + typeName + " does not exist");
	}

	// ----- protected methods -----
	protected GraphObject getEntity(final SecurityContext securityContext, final String typeName, final String uuid) throws FrameworkException {

		if (cachedEntity != null) {
			return cachedEntity;
		}

		final App app = StructrApp.getInstance(securityContext);
		final Traits traits = Traits.of(typeName);

		if (traits == null) {

			if (uuid != null) {

				throw new FrameworkException(404, "Type ‛" + typeName + "‛ does not exist");

			} else {

				throw new FrameworkException(400, "Request specifies no value for type and entity ID");
			}
		}


		if (traits.isNodeType()) {

			final NodeInterface entity = app.nodeQuery(typeName).uuid(uuid).getFirst();
			if (entity != null) {

				cachedEntity = entity;

				return entity;
			}

		} else {

			final RelationshipInterface entity = app.relationshipQuery(typeName).uuid(uuid).getFirst();
			if (entity != null) {

				cachedEntity = entity;

				return entity;
			}
		}

		throw new FrameworkException(404, "Entity with ID ‛" + uuid + "‛ of type ‛" +  typeName +  "‛ does not exist");
	}

	protected String buildLocationHeader(final SecurityContext securityContext, final org.structr.core.GraphObject newObject) {

		final StringBuilder uriBuilder = securityContext.getBaseURI();

		uriBuilder.append(getURL());
		uriBuilder.append("/");

		if (newObject != null) {

			uriBuilder.append(newObject.getUuid());
		}

		return uriBuilder.toString();
	}

	protected void applyDefaultSorting(final List<? extends org.structr.core.GraphObject> list, final SortOrder sortOrder) {

		if (sortOrder != null && !sortOrder.isEmpty()) {

			Collections.sort(list, sortOrder);
		}
	}

	protected void collectSearchAttributes(final SecurityContext securityContext, final String entityClass, final Query query) throws FrameworkException {

		final HttpServletRequest request = securityContext.getRequest();

		// first step: extract searchable attributes from request
		extractSearchableAttributes(securityContext, entityClass, request, query);

		// second step: distance search?
		extractDistanceSearch(request, query);
	}

	protected void extractDistanceSearch(final HttpServletRequest request, final Query query) {

		if (request != null) {

			final String distance = request.getParameter(RequestKeywords.Distance.keyword());

			if (!request.getParameterMap().isEmpty() && StringUtils.isNotBlank(distance)) {

				final String latlon   = request.getParameter(RequestKeywords.LatLon.keyword());
				if (latlon != null) {

					final String[] parts = latlon.split("[,]+");
					if (parts.length == 2) {

						try {
							final double dist      = Double.parseDouble(distance);
							final double latitude  = Double.parseDouble(parts[0]);
							final double longitude = Double.parseDouble(parts[1]);

							query.location(latitude, longitude, dist);

						} catch (NumberFormatException nex) {
							logger.warn("Unable to parse latitude, longitude or distance for search query {}", latlon);
						}
					}

				} else {

					final double dist     = Double.parseDouble(distance);
					final String location = request.getParameter(RequestKeywords.Location.keyword());

					String street     = request.getParameter(RequestKeywords.Street.keyword());
					String house      = request.getParameter(RequestKeywords.House.keyword());
					String postalCode = request.getParameter(RequestKeywords.PostalCode.keyword());
					String city       = request.getParameter(RequestKeywords.City.keyword());
					String state      = request.getParameter(RequestKeywords.State.keyword());
					String country    = request.getParameter(RequestKeywords.Country.keyword());

					// if location, use city and street, else use all fields that are there!
					if (location != null) {

						String[] parts = location.split("[,]+");
						switch (parts.length) {

							case 3:
								country = parts[2];	// no break here intentionally

							case 2:
								city = parts[1];	// no break here intentionally

							case 1:
								street = parts[0];
								break;

							default:
								break;
						}
					}

					query.location(street, house, postalCode, city, state, country, dist);
				}
			}
		}
	}

	protected void extractSearchableAttributes(final SecurityContext securityContext, final String type, final HttpServletRequest request, final Query query) throws FrameworkException {

		if (type != null && request != null && !request.getParameterMap().isEmpty()) {

			if (request.getParameter(RequestKeywords.Inexact_Deprecated.keyword()) != null) {

				logger.warn("Usage of deprecated request keyword {} detected. This keyword is deprecated and will be removed in the future. Please use {} instead.",
					RequestKeywords.Inexact_Deprecated.keyword(),
					RequestKeywords.Inexact.keyword()
				);
			}

			boolean exactSearch                = !getTruthyValueFromRequestParameters(request, RequestKeywords.Inexact_Deprecated, RequestKeywords.Inexact);
			final List<PropertyKey> searchKeys = new LinkedList<>();
			final Traits traits                = Traits.of(type);

			for (final String name : request.getParameterMap().keySet()) {

				final String firstPart = getFirstPartOfString(name);
				if (traits.hasKey(firstPart)) {

					final PropertyKey key = traits.key(firstPart);

					// add to list of searchable keys
					searchKeys.add(key);

				} else if (!RequestKeywords.keywords().contains(name)) {

					// exclude common request parameters here (should not throw exception)
					throw new FrameworkException(400, "Unknown search key " + name);
				}
			}

			// sort list of search keys according to their desired order
			// so that querying search attributes can use other attributes
			// to refine their partial results.
			Collections.sort(searchKeys, new PropertyKeyProcessingOrderComparator());

			for (final PropertyKey key : searchKeys) {

				// hand list of search attributes over to key
				key.extractSearchableAttribute(securityContext, request, exactSearch, query);
			}
		}
	}

	protected RestMethodResult wrapInResult(final Object obj) {

		RestMethodResult result = null;

		if (obj instanceof RestMethodResult r) {

			result = r;

		} else {

			result = new RestMethodResult(200);
			result.addContent(obj);

			if (obj instanceof Collection c) {

				result.setOverridenResultCount(c.size());
			}

		}

		return result;
	}

	protected static int parseInteger(final Object source) {

		try {
			return Integer.parseInt(source.toString());

		} catch (final Throwable t) {}

		return -1;
	}

	protected boolean isDefaultView() {
		return PropertyView.Public.equals(requestedView);
	}

	public RestMethodResult genericPut(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

		final List<org.structr.core.GraphObject> results = Iterables.toList(doGet(securityContext, null, NodeFactory.DEFAULT_PAGE_SIZE, NodeFactory.DEFAULT_PAGE));

		if (results != null && !results.isEmpty()) {

			final Traits traits = results.get(0).getTraits();

			// instruct deserialization strategies to set properties on related nodes
			securityContext.setAttribute("setNestedProperties", true);

			PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, traits.getName(), propertySet);

			for (final org.structr.core.GraphObject obj : results) {

				if (obj.isNode() && !obj.getSyncNode().isGranted(Permission.write, securityContext)) {

					throw new FrameworkException(403, PropertyContainerTraitDefinition.getModificationNotPermittedExceptionString(obj, securityContext));
				}

				obj.setProperties(securityContext, properties);
			}

			return new RestMethodResult(HttpServletResponse.SC_OK);
		}

		throw new IllegalPathException(getURL() + " can only be applied to a non-empty resource");
	}

	protected RestMethodResult genericDelete(final SecurityContext securityContext) throws FrameworkException {

		final App app      = StructrApp.getInstance(securityContext);
		final int pageSize = 500;
		int count          = 0;
		int chunk          = 0;
		boolean hasMore    = true;

		while (hasMore) {

			// will be set to true below if at least one result was processed
			hasMore = false;

			try (final Tx tx = app.tx(true, true, false)) {

				chunk++;

				// always fetch the first page
				try (final ResultStream<org.structr.core.GraphObject> result = doGet(securityContext, null, pageSize, 1)) {

					for (final org.structr.core.GraphObject obj : result) {

						if (obj.isNode()) {

							final NodeInterface node = (NodeInterface)obj;

							if (!TransactionCommand.isDeleted(node.getNode())) {

								app.delete(node);
								hasMore = true;
							}

						} else {

							final RelationshipInterface relationship = (RelationshipInterface)obj;

							if (!TransactionCommand.isDeleted(relationship.getRelationship())) {

								app.delete(relationship);
								hasMore = true;
							}
						}

						count++;
					}
				}

				tx.success();

				logger.info("DeleteObjects: {} objects processed", count);

			} catch (FrameworkException nfex) {

				// ignore NotFoundException

			} catch (Throwable t) {

				logger.warn("Exception in DELETE chunk #{}: {}", chunk, t.toString());

				// we need to break here, otherwise the delete call will loop
				// endlessly, trying to delete the erroneous objects.
				break;
			}
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	protected RestMethodResult genericPatch(final SecurityContext securityContext, final List<Map<String, Object>> propertySets,final String typeName) throws FrameworkException {

		final RestMethodResult result                = new RestMethodResult(HttpServletResponse.SC_OK);
		final App app                                = StructrApp.getInstance(securityContext);
		final Iterator<Map<String, Object>> iterator = propertySets.iterator();
		final int batchSize                          = intOrDefault(RequestKeywords.BatchSize.keyword(), 1000);
		int overallCount                             = 0;

		while (iterator.hasNext()) {

			try (final Tx tx = app.tx()) {

				int count = 0;

				while (iterator.hasNext() && count++ < batchSize) {

					final Map<String, Object> propertySet = iterator.next();
					String localType                      = typeName;

					overallCount++;

					// determine type of object
					final Object typeSource = propertySet.get("type");
					if (typeSource != null && typeSource instanceof String) {

						final String typeString = (String)typeSource;
						final Traits traits     = Traits.of(typeString);
						if (traits != null) {

							localType = traits.getName();
						}
					}

					// find object by id, apply PATCH
					final Object idSource = propertySet.get("id");
					if (idSource != null) {

						if (idSource instanceof String) {

							final String id = (String)idSource;

							org.structr.core.GraphObject obj = app.getNodeById(localType, id);
							if (obj == null) {

								obj = app.getRelationshipById(localType, id);
							}

							if (obj != null) {

								// test
								localType = obj.getType();

								propertySet.remove("id");

								final PropertyMap data = PropertyMap.inputTypeToJavaType(securityContext, localType, propertySet);

								obj.setProperties(securityContext, data);

							} else {

								throw new FrameworkException(404, "Object with ID " + id + " not found.");
							}

						} else {

							throw new FrameworkException(422, "Invalid PATCH input, object id must be of type string.");
						}

					} else {

						createNode(securityContext, typeName, propertySet);
					}
				}

				logger.info("Committing PATCH transaction batch, {} objects processed.", overallCount);

				tx.success();
			}
		}

		return result;
	}

	// ----- private methods -----
	/**
	 * Returns the first part of the given source string when it contains a "."
	 *
	 * @return source
	 */
	private String getFirstPartOfString(final String source) {

		final int pos = source.indexOf(".");
		if (pos > -1) {

			return source.substring(0, pos);
		}

		return source;
	}

	private int intOrDefault(final String source, final int defaultValue) {

		if (source != null) {

			try {

				return Integer.parseInt(source);

			} catch (Throwable t) {}

		}

		return defaultValue;
	}

	private boolean getTruthyValueFromRequestParameters(final HttpServletRequest request, final RequestKeywords... requestKeywords) {

		for (final RequestKeywords k : requestKeywords) {

			final String value = request.getParameter(k.keyword());
			if (value != null) {

				if (parseInteger(value) == 1) {
					return true;
				}

				if (parseBoolean(value)) {
					return true;
				}
			}
		}

		return false;
	}

	// ----- nested classes -----
	private static class PropertyKeyProcessingOrderComparator implements Comparator<PropertyKey> {

		@Override
		public int compare(final PropertyKey key1, final PropertyKey key2) {
			return Integer.valueOf(key1.getProcessingOrderPosition()).compareTo(Integer.valueOf(key2.getProcessingOrderPosition()));
		}
	}
}
