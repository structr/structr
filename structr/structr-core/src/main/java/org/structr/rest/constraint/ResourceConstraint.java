/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.ErrorBuffer;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
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

	protected SecurityContext securityContext = null;

	public abstract boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request);

	public abstract List<? extends GraphObject> doGet(List<VetoableGraphObjectListener> listeners) throws PathException;
	public abstract RestMethodResult doPost(final Map<String, Object> propertySet, final List<VetoableGraphObjectListener> listeners) throws Throwable;
	public abstract RestMethodResult doHead() throws Throwable;
	public abstract RestMethodResult doOptions() throws Throwable;
	public abstract String getUriPart();
	public abstract ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException;

	// ----- methods -----
	public final RestMethodResult doPut(final Map<String, Object> propertySet, final List<VetoableGraphObjectListener> listeners) throws Throwable {

		final List<? extends GraphObject> results = doGet(listeners);
		if(results != null && !results.isEmpty()) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					ErrorBuffer errorBuffer = new ErrorBuffer();
					for(GraphObject obj : results) {

						if(mayModify(listeners, obj, errorBuffer)) {

							for(Entry<String, Object> attr : propertySet.entrySet()) {
								obj.setProperty(attr.getKey(), attr.getValue());
							}
							
						} else {
							throw new IllegalArgumentException(errorBuffer.toString());
						}
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
	}

	public final RestMethodResult doDelete(final List<VetoableGraphObjectListener> listeners) throws Throwable {

		final List<? extends GraphObject> results = doGet(listeners);
		if(results != null && !results.isEmpty()) {

			Boolean success = (Boolean)Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					ErrorBuffer errorBuffer = new ErrorBuffer();
					boolean success = true;

					for(GraphObject obj : results) {

						if(mayDelete(listeners, obj, errorBuffer)) {

							// 1: delete relationships
							if(obj instanceof AbstractNode) {
								List<StructrRelationship> rels = ((AbstractNode)obj).getRelationships();
								for(StructrRelationship rel : rels) {
									success &= rel.delete();
								}
							}

							// 2: delete object
							success &= obj.delete();
						}
					}

					// roll back transaction if not all deletions were successful
					if(!success) {
						// throwable will cause transaction to be rolled back
						throw new IllegalArgumentException(errorBuffer.toString());
					}

					return success;
				}

			});

			if(success != null && success.booleanValue() == true) {
				return new RestMethodResult(HttpServletResponse.SC_OK);
			}
		}

		throw new NotFoundException();
	}

	/**
	 *
	 * @param propertyView
	 */
	public void configurePropertyView(Value<PropertyView> propertyView) {
	}

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	// ----- protected methods -----
	protected boolean mayCreate(List<VetoableGraphObjectListener> listeners, GraphObject object, ErrorBuffer errorBuffer) {

		boolean mayCreate = true;

		// only allow modification if all listeners answer with "yes"
		for(VetoableGraphObjectListener listener : listeners) {
			mayCreate &= listener.mayCreate(object, securityContext, errorBuffer);
		}

		return mayCreate;
	}

	protected boolean mayModify(List<VetoableGraphObjectListener> listeners, GraphObject object, ErrorBuffer errorBuffer) {

		boolean mayModify = true;

		// only allow modification if all listeners answer with "yes"
		for(VetoableGraphObjectListener listener : listeners) {
			mayModify &= listener.mayModify(object, securityContext, errorBuffer);
		}

		return mayModify;
	}

	protected boolean mayDelete(List<VetoableGraphObjectListener> listeners, GraphObject object, ErrorBuffer errorBuffer) {

		boolean mayDelete = true;

		// only allow deletion if all listeners answer with "yes"
		for(VetoableGraphObjectListener listener : listeners) {
			mayDelete &= listener.mayDelete(object, securityContext, errorBuffer);
		}

		return mayDelete;
	}

	protected void notifyOfTraversal(List<VetoableGraphObjectListener> listeners, List<GraphObject> traversedNodes) {
		for(VetoableGraphObjectListener listener : listeners) {
			listener.notifyOfTraversal(traversedNodes, securityContext);
		}
	}

	protected String buildLocationHeader(String type, long id) {

		StringBuilder uriBuilder = securityContext.getBaseURI();

		uriBuilder.append(getUriPart());
		uriBuilder.append("/");
		uriBuilder.append(id);

		return uriBuilder.toString();
	}
}
