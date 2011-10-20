/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.rest.constraint;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.VetoableGraphObjectListener;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NotFoundException;
import org.structr.rest.exception.PathException;
import org.structr.rest.wrapper.PropertySet;

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

	public abstract List<GraphObject> doGet() throws PathException;
	public abstract RestMethodResult doPost(final PropertySet propertySet, final List<VetoableGraphObjectListener> listeners) throws Throwable;
	public abstract RestMethodResult doHead() throws Throwable;
	public abstract RestMethodResult doOptions() throws Throwable;

	public abstract boolean checkAndConfigure(String part, HttpServletRequest request);
	public abstract ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException;

	// ----- methods -----
	public final RestMethodResult doPut(final PropertySet propertySet, final List<VetoableGraphObjectListener> listeners) throws Throwable {

		final List<GraphObject> results = doGet();
		if(results != null && !results.isEmpty()) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					for(GraphObject obj : results) {

						if(mayModify(listeners, obj)) {

							for(NodeAttribute attr : propertySet.getAttributes()) {
								obj.setProperty(attr.getKey(), attr.getValue());
							}
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

		final List<GraphObject> results = doGet();
		if(results != null && !results.isEmpty()) {

			Boolean success = (Boolean)Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					boolean success = true;
					for(GraphObject obj : results) {

						if(mayDelete(listeners, obj)) {
							success &= obj.delete();
						}
					}

					// roll back transaction if not all deletions were successful
					if(!success) {
						// throwable will cause transaction to be rolled back
						throw new IllegalStateException("Deletion failed, roll back transaction");
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
	protected boolean mayModify(List<VetoableGraphObjectListener> listeners, GraphObject object) {

		boolean mayModify = true;

		// only allow modification if all listeners answer with "yes"
		for(VetoableGraphObjectListener listener : listeners) {
			mayModify &= listener.mayModify(object, securityContext);
		}

		return mayModify;
	}

	protected boolean mayDelete(List<VetoableGraphObjectListener> listeners, GraphObject object) {

		boolean mayDelete = true;

		// only allow deletion if all listeners answer with "yes"
		for(VetoableGraphObjectListener listener : listeners) {
			mayDelete &= listener.mayDelete(object, securityContext);
		}

		return mayDelete;
	}

	protected String buildCreatedURI(HttpServletRequest request, String type, long id) {

		StringBuilder uriBuilder = new StringBuilder(100);
		uriBuilder.append(request.getScheme());
		uriBuilder.append("://");
		uriBuilder.append(request.getServerName());
		uriBuilder.append(":");
		uriBuilder.append(request.getServerPort());
		uriBuilder.append(request.getContextPath());
		uriBuilder.append(request.getServletPath());
		uriBuilder.append("/");

		if(type != null) {
			uriBuilder.append(type.toLowerCase());
			uriBuilder.append("s/");
		}

		uriBuilder.append(id);

		return uriBuilder.toString();
	}
}
