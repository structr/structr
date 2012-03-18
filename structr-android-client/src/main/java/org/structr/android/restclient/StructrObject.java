/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.android.restclient;

import android.net.http.AndroidHttpClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.entity.StringEntity;

/**
 * An abstract base class for REST entities on a structr server. This class encapsulates everything
 * neccesary to interact with a structr REST server in a single, convenient interface. Derive your
 * entities from this class, and you can easily create, load, update and delete them on a structr
 * server. 
 *
 * @author Christian Morgner
 */
public abstract class StructrObject {

	private static final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create();	
	private static AndroidHttpClient client = null;
	
	@Expose
	private String id = null;

	/**
	 * Override this method to load additional resources after
	 * the entity has been created from the JSON source. You can
	 * for example use this method to load nested fields of an
	 * entity synchronously.
	 */
	public void onDbLoad() {
	}
	
	/**
	 * @return the database ID of this entity
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the ID of this entity. This method can be used to create an entity
	 * with pre-set ID.
	 * @param id the id of this entity
	 */
	public void setId(String id) {
		this.id = id;
	}
	
	/**
	 * @return whether the REST server knows about the existance of this entity.
	 */
	public boolean isPersistent() {
		return id != null;
	}
	
	/**
	 * Creates this entity on the REST server. After successful creation, the ID
	 * of this entity will be set.
	 * 
	 * @throws Throwable 
	 */
	public void dbCreate() throws Throwable {
		create(buildPath("/", getEntityName()), this, getClass());
	}
	
	/**
	 * Stores the exposed attributes of this entity on the REST server.
	 * 
	 * @throws Throwable 
	 */
	public void dbStore() throws Throwable {
		store(buildPath("/", getEntityName(), "/", getId()), this, getClass());
	}
	
	/**
	 * Deletes this entity from the REST server. After successful deletion, the ID
	 * if this entity is null.
	 * 
	 * @throws Throwable 
	 */
	public void dbDelete() throws Throwable {
		delete(buildPath("/", getEntityName(), "/", getId()));
	}
	
	/**
	 * Loads an entity with the given type and ID from the REST server.
	 * 
	 * @param type the type of the entity to load
	 * @param id the ID of the entity to load
	 * @return the entity from the REST server, or null if the entity was not found
	 * @throws Throwable 
	 */
	public static StructrObject dbGet(Class type, String id) throws Throwable {
		
		StructrObject newInstance = newInstance(type);
		if(newInstance != null) {
			return load(type, buildPath("/", newInstance.getEntityName(), "/", id));
		}
		
		return null;
	}
	
	/**
	 * Loads an entity with the given property value from the REST server.
	 * 
	 * @param type the type of the entity to load
	 * @param key the property key
	 * @param value the property value
	 * @return the entity from the REST server, or null if the entity was not found
	 * @throws Throwable 
	 */
	public static StructrObject dbLoad(Class type, String key, Object value) throws Throwable {
		
		StructrObject newInstance = newInstance(type);
		if(newInstance != null) {
			return load(type, buildPath("/", newInstance.getEntityName(), "?", key, "=", value));
		}
		
		return null;
	}
	
	/**
	 * Loads an entity from the given path. Use this method to load entities from
	 * arbitraty paths.
	 * 
	 * @param type the type of the entity to load
	 * @param path the path of the entity to load
	 * @return the entity from the REST server, or null if the entity was not found
	 * @throws Throwable 
	 */
	public static StructrObject dbLoad(Class type, String path) throws Throwable {
		
		StructrObject newInstance = newInstance(type);
		if(newInstance != null) {
			return load(type, buildPath(path));
		}
		
		return null;
	}
	
	/**
	 * Fetches a sorted list of entities with the given type from the REST server.
	 * 
	 * @param type the type of the entities to load
	 * @param sortKey the sort key
	 * @param asc whether to sort ascending or descending
	 * @param params additional parameters, may be empty
	 * @return a sorted list of entities matching the given type and parameters
	 * @throws Throwable 
	 */
	public static List<? extends StructrObject> dbList(Class type, String sortKey, boolean asc, Object... params) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if(newInstance != null) {
			return list(type, buildPath("/", newInstance.getEntityName(), "?sort=", sortKey, asc ? "" : "&order=desc", params));
		}
		
		return null;
	}
	
	/**
	 * Fetches a list of entities from the given path. Use this method to fetch arbitrary collections.
	 * 
	 * @param type the type of the entities to load
	 * @param path the path of the entities to load
	 * @return a list of entities from the given path
	 * @throws Throwable 
	 */
	public static List<? extends StructrObject> dbList(Class type, String path) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if(newInstance != null) {
			return list(type, buildPath(path));
		}
		
		return null;
	}
	
	/**
	 * Fetches a sorted list of entities with the given type and property value from the REST server.
	 * 
	 * @param type the type of the entities to load
	 * @param key the property key to search for
	 * @param value the property value to search for
	 * @param sortKey the sort key
	 * @param asc whether to sort ascending or descending
	 * @return a sorted list of entities matching the given type and property value
	 * @throws Throwable 
	 */
	public static List<? extends StructrObject> dbFind(Class type, String key, Object value, String sortKey, boolean asc) throws Throwable {

		StructrObject newInstance = newInstance(type);
		if(newInstance != null) {
			return list(type, buildPath("/", newInstance.getEntityName(), "?", key, "=", value, "&sort=", sortKey, asc ? "" : "&order=desc"));
		}
		
		return null;
	}

	/**
	 * Fetches a sorted list of child entities for a given parent from the REST server.
	 * 
	 * @param type the parent's type
	 * @param id the parent's ID
	 * @param childType the children's type
	 * @param sortKey the sort key
	 * @param asc whether to sort ascending or descending
	 * @return a sorted list of entities that are children of a given parent entity with the given ID
	 * @throws Throwable 
	 */
	public static List<? extends StructrObject> dbFind(Class type, String id, Class childType, String sortKey, boolean asc) throws Throwable {

		StructrObject childInstance = newInstance(childType);
		StructrObject newInstance = newInstance(type);
		if(newInstance != null) {
			return list(childType, buildPath("/", newInstance.getEntityName(), "/", id, "/", childInstance.getEntityName(), "?sort=", sortKey, asc ? "" : "&order=desc"));
		}
		
		return null;
	}

	/**
	 * Fetches a single child entitiy with a given ID from a parent with a given ID. This method can
	 * be used to check for an existing relationship between the to entities. If a valid path exists
	 * from one entity to the other, the entities are related.
	 * 
	 * @param type the parent's type
	 * @param id the parent's ID
	 * @param childType the child's type
	 * @param childId the child's ID
	 * @param sortKey the sort key
	 * @param asc whether to sort ascending or descending
	 * @return the child entity with the given ID, if there is a relationship with the parent
	 * @throws Throwable 
	 */
	public static StructrObject dbFind(Class type, String id, Class childType, String childId, String sortKey, boolean asc) throws Throwable {

		StructrObject childInstance = newInstance(childType);
		StructrObject newInstance = newInstance(type);
		if(newInstance != null) {
			return load(childType, buildPath("/", newInstance.getEntityName(), "/", id, "/", childInstance.getEntityName(), "/", childId));
		}
		
		return null;
	}

	/**
	 * Shuts down the the http client that is used for the database connection.
	 */
	public static void shutdownDatabaseConnection() {
		
		if(client != null) {
			client.getConnectionManager().shutdown();
			client.close();
			client = null;
		}
	}
	
	/**
	 * @return the http client that is used to connect to the REST server.
	 */
	public static AndroidHttpClient getHttpClient() {
		
		if(client == null) {
			    
			client = AndroidHttpClient.newInstance("structr REST client");
			client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", StructrConnector.getSocketFactory(), 8088));
		}
		
		return client;
	}
	
	// ----- private methods -----
	private String getEntityName() {
		return getClass().getSimpleName().toLowerCase();
	}
		
	
	// ----- private static methods -----
	private static StructrObject load(Class type, String path) throws Throwable {
		
		AndroidHttpClient httpClient = getHttpClient();
		HttpGet httpGet = new HttpGet(path);
		configureRequest(httpGet);
		HttpResponse response = null;
		StructrObject result = null;
		Throwable throwable = null;
		
		try {

			response = httpClient.execute(httpGet);
			
			if(response.getStatusLine().getStatusCode() == 200) {
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StructrEntityResult<StructrObject> entityResult = (StructrEntityResult)gson.fromJson(reader, getEntityTypeToken(type));
				reader.close();

				if(entityResult != null) {
					result = entityResult.getResult();
					result.onDbLoad();
				}
				
			} else {
				throw new StructrException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
			}
			
		} catch(Throwable t) {
			throwable = t;
			httpGet.abort();
		} finally {
			
			if(response != null) {
				response.getEntity().consumeContent();
			}
		}
		
		if(throwable != null) {
			throw throwable;
		}
		
		return result;
	}
	
	private static int create(String path, StructrObject entity, Type type) throws Throwable {
		
		AndroidHttpClient httpClient = getHttpClient();
		HttpPost httpPost = new HttpPost(path);
		HttpResponse response = null;
		Throwable throwable = null;
		int responseCode = 0;

		try {
			StringBuilder buf = new StringBuilder();
			gson.toJson(entity, type, buf);

			StringEntity body = new StringEntity(buf.toString(), "UTF-8");
			body.setContentType("application/json");
			httpPost.setEntity(body);
			configureRequest(httpPost);

			response = httpClient.execute(httpPost);
			responseCode = response.getStatusLine().getStatusCode();
			if(responseCode == 201) {

				String location = response.getFirstHeader("Location").getValue();
				String newId = getIdFromLocation(location);

				// only set ID of it's not already set
				if(entity.getId() == null) {
					entity.setId(newId);
				}
				
			} else {
				throw new StructrException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
			}
			
		} catch(Throwable t) {
			throwable = t;
		} finally {
			if(response != null) {
				response.getEntity().consumeContent();
			}
		}
		
		if(throwable != null) {
			throw throwable;
		}
			
		return responseCode;
	}
	
	private static int store(String path, StructrObject entity, Type type) throws Throwable {
		
		AndroidHttpClient httpClient = getHttpClient();
		HttpPut httpPut = new HttpPut(path);
		HttpResponse response = null;
		Throwable throwable = null;
		int responseCode = 0;

		try {
			StringBuilder buf = new StringBuilder();
			gson.toJson(entity, type, buf);

			StringEntity body = new StringEntity(buf.toString());
			body.setContentType("application/json");
			httpPut.setEntity(body);
			configureRequest(httpPut);

			response = httpClient.execute(httpPut);
			responseCode = response.getStatusLine().getStatusCode();
			
		} catch(Throwable t) {
			throwable = t;
			httpPut.abort();
		} finally {
			if(response != null) {
				response.getEntity().consumeContent();
			}
		}
		
		if(throwable != null) {
			throw throwable;
		}
		
		return responseCode;
	}
	
	private static int delete(String path) throws Throwable {
		
		AndroidHttpClient httpClient = getHttpClient();
		HttpDelete delete = new HttpDelete(path);
		configureRequest(delete);
		HttpResponse response = null;
		Throwable throwable = null;
		int responseCode = 0;

		try {
			response = httpClient.execute(delete);
			responseCode = response.getStatusLine().getStatusCode();
			
		} catch(Throwable t) {
			throwable = t;
			delete.abort();
		} finally {
			if(response != null) {
				response.getEntity().consumeContent();
			}
		}
		
		if(throwable != null) {
			throw throwable;
		}
		
		return responseCode;
	}

	private static List list(Class type, String path) throws Throwable {

		AndroidHttpClient httpClient = getHttpClient();
		HttpGet httpGet = new HttpGet(path);
		configureRequest(httpGet);
		List<StructrObject> result = null;
		HttpResponse response = null;
		Throwable throwable = null;
		
		try {

			response = httpClient.execute(httpGet);
			
			if(response.getStatusLine().getStatusCode() == 200) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				StructrCollectionResult<StructrObject> collectionResult = (StructrCollectionResult)gson.fromJson(reader, getCollectionTypeToken(type));
				reader.close();

				if(collectionResult != null) {
					result = collectionResult.getResult();
					for(StructrObject obj : result) {
						obj.onDbLoad();
					}
				} else {
					result = Collections.emptyList();
				}
				
			} else {
				throw new StructrException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
			}
		
		} catch(Throwable t) {
			throwable = t;
			httpGet.abort();
			
		} finally {
			if(response != null) {
				response.getEntity().consumeContent();
			}
		}
		
		if(throwable != null) {
			throw throwable;
		}
		
		return result;
	}

	private static String buildPath(String url, Object... params) {
		
		StringBuilder path = new StringBuilder();
		String base = StructrConnector.getServer();
		
		path.append(base);
		if(!base.endsWith("/")) {
			path.append("/");
		}
		path.append(url);
		
		for(Object o : params) {
			
			if(o.getClass().isArray()) {
				Object[] array = (Object[])o;
				for(Object a : array) {
					path.append(a);
				}
				
			} else {
				path.append(o);
			}
		}
		
		return path.toString();
	}

	private static void configureRequest(HttpRequest request) {
		
		request.addHeader("X-BillardUser", StructrConnector.getUserName());
		request.addHeader("X-BillardPassword", StructrConnector.getPassword());
	}
	
	private static String getIdFromLocation(String location) {
		int pos = location.lastIndexOf("/");
		return location.substring(pos+1);
	}

	private static StructrObject newInstance(Class type) {
		try { return (StructrObject)type.newInstance(); } catch(Throwable t) {}
		return null;
	}
		
	private static Type getCollectionTypeToken(Class type) {
		return new ParameterizedTypeImpl(StructrCollectionResult.class, type);
	}
	
	private static Type getEntityTypeToken(Class type) {
		return new ParameterizedTypeImpl(StructrEntityResult.class, type);
	}
	
	// ----- private static nested classes -----
	private static class ParameterizedTypeImpl implements ParameterizedType {

		private Class genericType = null;
		private Class rawType = null;
		
		public ParameterizedTypeImpl(Class rawType, Class genericType) {
			this.rawType = rawType;
			this.genericType = genericType;
		}
		
		public Type[] getActualTypeArguments() {
			return new Type[] { genericType };
		}

		public Type getOwnerType() {
			return StructrObject.class;
		}

		public Type getRawType() {
			return rawType;
		}

		@Override
		public boolean equals(Object o) {
			return o.hashCode() == this.hashCode();
		}
		
		@Override
		public int hashCode() {
			return (genericType.hashCode() * 31) + rawType.hashCode();
		}
	}

	private static class StructrEntityResult<T extends StructrObject> {

		@Expose
		T result = null;

		public void setResult(T result) {
			this.result = result;
		}

		public T getResult() {
			return result;
		}

	}
	
	private class StructrCollectionResult<T extends StructrObject> {

		@Expose
		List<T> result = null;

		public void setResult(List<T> result) {
			this.result = result;
		}

		public List<T> getResult() {
			return result;
		}
	}
}
