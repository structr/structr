/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.RemoveNodeFromIndex;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.core.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.exception.PathException;

/**
 * Base class for all resource constraints. Constraints can be
 * combined with succeeding constraints to avoid unneccesary
 * evaluation.
 *
 * 
 * @author Christian Morgner
 */
public abstract class ResourceConstraint {

	private static final Logger logger = Logger.getLogger(ResourceConstraint.class.getName());
	
	protected SecurityContext securityContext = null;
	protected String idProperty = null;

	public abstract boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request);

	public abstract List<? extends GraphObject> doGet() throws PathException;
	public abstract RestMethodResult doPost(final Map<String, Object> propertySet) throws Throwable;
	public abstract RestMethodResult doHead() throws Throwable;
	public abstract RestMethodResult doOptions() throws Throwable;
	public abstract String getUriPart();
	public abstract ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException;
	public abstract boolean isCollectionResource();

	// ----- methods -----
	public RestMethodResult doDelete() throws Throwable {

		Iterable<? extends GraphObject> results;

		// catch 204, DELETE must return 200 if resource is empty
		try { results = doGet(); } catch(NoResultsException nre) { results = null; }

		if(results != null) {

			final Iterable<? extends GraphObject> finalResults = results;

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					for(GraphObject obj : finalResults) {

						// 1: remove node from index
						Services.command(securityContext, RemoveNodeFromIndex.class).execute(obj);

						// 2: delete relationships
						if(obj instanceof AbstractNode) {
							List<StructrRelationship> rels = ((AbstractNode)obj).getRelationships();
							for(StructrRelationship rel : rels) {
								rel.delete(securityContext);
							}
						}

						// 3: delete object
						obj.delete(securityContext);
					}

					return null;
				}

			});
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
	}

	public RestMethodResult doPut(final Map<String, Object> propertySet) throws Throwable {

		if(securityContext != null && securityContext.getUser() != null) {
			
			final Iterable<? extends GraphObject> results = doGet();
			if(results != null) {

				StructrTransaction transaction = new StructrTransaction() {

					@Override
					public Object execute() throws Throwable {

						ErrorBuffer errorBuffer = new ErrorBuffer();
						boolean error = false;

						for(GraphObject obj : results) {

							for(Entry<String, Object> attr : propertySet.entrySet()) {

								try {
									if(attr.getValue() != null) {
										obj.setProperty(attr.getKey(), attr.getValue());
									} else {
										obj.removeProperty(attr.getKey());
									}

								} catch(Throwable t) {
									errorBuffer.add(t.getMessage());
									error = true;
								}
							}

							/*
							try {
								// ask listener for modification validation
								EntityContext.getGlobalModificationListener().graphObjectModified(securityContext, obj);

							} catch(Throwable t) {
								errorBuffer.add(t.getMessage());
								error = true;
							}
							*/
						}

						// throw exception with errors
						if(error) {
							throw new IllegalArgumentException(errorBuffer.toString());
						}

						return null;
					}

				};

				// modify results in a single transaction
				Services.command(securityContext, TransactionCommand.class).execute(transaction);

				// if there was an exception, throw it again
				if(transaction.getCause() != null) {
					throw transaction.getCause();
				}

				return new RestMethodResult(HttpServletResponse.SC_OK);
			}

			throw new IllegalPathException();

		} else {

			throw new NotAllowedException();
		}
	}

	/**
	 *
	 * @param propertyView
	 */
	public void configurePropertyView(Value<String> propertyView) {
	}

	public void configureIdProperty(String idProperty) {
		this.idProperty = idProperty;
	}

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	// ----- protected methods -----
	protected void notifyOfTraversal(List<GraphObject> traversedNodes) {
		for(VetoableGraphObjectListener listener : EntityContext.getModificationListeners()) {
			listener.wasVisited(traversedNodes, -1, securityContext);
		}
	}

	protected String buildLocationHeader(GraphObject newObject) {

		StringBuilder uriBuilder = securityContext.getBaseURI();

		uriBuilder.append(getUriPart());
		uriBuilder.append("/");

		if(newObject != null) {

			// use configured id property
			if(idProperty == null) {
				uriBuilder.append(newObject.getId());
			} else {
				uriBuilder.append(newObject.getProperty(idProperty));
			}
		}

		return uriBuilder.toString();
	}
}
