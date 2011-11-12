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
import org.structr.rest.exception.NoResultsException;
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

	public abstract boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request);

	public abstract List<? extends GraphObject> doGet(final List<VetoableGraphObjectListener> listeners) throws PathException;
	public abstract RestMethodResult doPost(final Map<String, Object> propertySet, final List<VetoableGraphObjectListener> listeners) throws Throwable;
	public abstract RestMethodResult doHead() throws Throwable;
	public abstract RestMethodResult doOptions() throws Throwable;
	public abstract String getUriPart();
	public abstract ResourceConstraint tryCombineWith(ResourceConstraint next) throws PathException;
	public abstract boolean isCollectionResource();

	// ----- methods -----
	public final RestMethodResult doPut(final Map<String, Object> propertySet, final List<VetoableGraphObjectListener> listeners) throws Throwable {

		final Iterable<? extends GraphObject> results = doGet(listeners);
		if(results != null) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					ErrorBuffer errorBuffer = new ErrorBuffer();
					boolean error = false;

					for(GraphObject obj : results) {

						if(mayModify(listeners, obj, errorBuffer)) {

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
							
						} else {
							
							throw new IllegalArgumentException(errorBuffer.toString());
						}

						// ask listener for modification validation
						if(!validAfterModification(listeners, obj, errorBuffer) || error) {
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

		Iterable<? extends GraphObject> results;

		// catch 204, DELETE must return 200 if resource is empty
		try { results = doGet(listeners); } catch(NoResultsException nre) { results = null; }

		if(results != null) {

			final Iterable<? extends GraphObject> finalResults = results;

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws Throwable {

					ErrorBuffer errorBuffer = new ErrorBuffer();
					boolean success = true;

					for(GraphObject obj : finalResults) {

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
		}

		return new RestMethodResult(HttpServletResponse.SC_OK);
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

	protected boolean validAfterCreation(List<VetoableGraphObjectListener> listeners, GraphObject object, ErrorBuffer errorBuffer) {

		boolean valid = true;

		for(VetoableGraphObjectListener listener : listeners) {
			valid &= listener.validAfterCreation(object, securityContext, errorBuffer);
		}

		return valid;
	}

	protected boolean mayModify(List<VetoableGraphObjectListener> listeners, GraphObject object, ErrorBuffer errorBuffer) {

		boolean mayModify = true;

		// only allow modification if all listeners answer with "yes"
		for(VetoableGraphObjectListener listener : listeners) {
			mayModify &= listener.mayModify(object, securityContext, errorBuffer);
		}

		return mayModify;
	}

	protected boolean validAfterModification(List<VetoableGraphObjectListener> listeners, GraphObject object, ErrorBuffer errorBuffer) {

		boolean valid = true;

		for(VetoableGraphObjectListener listener : listeners) {
			valid &= listener.validAfterModification(object, securityContext, errorBuffer);
		}
		
		return valid;
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
			listener.wasVisited(traversedNodes, securityContext);
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
