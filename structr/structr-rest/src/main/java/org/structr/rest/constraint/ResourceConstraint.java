
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package org.structr.rest.constraint;

import org.structr.common.error.ErrorBuffer;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.VetoableGraphObjectListener;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.node.RemoveNodeFromIndex;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.IllegalPathException;
import org.structr.rest.exception.NoResultsException;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.DirectedRelationship;

//~--- classes ----------------------------------------------------------------

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

	//~--- fields ---------------------------------------------------------

	protected String idProperty               = null;
	protected SecurityContext securityContext = null;

	//~--- methods --------------------------------------------------------

	public abstract boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request);

	public abstract List<? extends GraphObject> doGet() throws FrameworkException;

	public abstract RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException;

	public abstract RestMethodResult doHead() throws FrameworkException;

	public abstract RestMethodResult doOptions() throws FrameworkException;

	public abstract ResourceConstraint tryCombineWith(ResourceConstraint next) throws FrameworkException;

	// ----- methods -----
	public RestMethodResult doDelete() throws FrameworkException {

		Iterable<? extends GraphObject> results;

		// catch 204, DELETE must return 200 if resource is empty
		try {
			results = doGet();
		} catch (NoResultsException nre) {
			results = null;
		}

		if (results != null) {

			final Iterable<? extends GraphObject> finalResults = results;

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					for (GraphObject obj : finalResults) {

						// 1: remove node from index
						Services.command(securityContext, RemoveNodeFromIndex.class).execute(obj);

						// 2: delete relationships
						if (obj instanceof AbstractNode) {

							List<StructrRelationship> rels = ((AbstractNode) obj).getRelationships();

							for (StructrRelationship rel : rels) {

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

	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {

		final Iterable<? extends GraphObject> results = doGet();

		if (results != null) {

			StructrTransaction transaction = new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					for (GraphObject obj : results) {

						for (Entry<String, Object> attr : propertySet.entrySet()) {
							obj.setProperty(attr.getKey(), attr.getValue());
						}
					}

					return null;
				}
			};

			// modify results in a single transaction
			Services.command(securityContext, TransactionCommand.class).execute(transaction);

			return new RestMethodResult(HttpServletResponse.SC_OK);

		}

		throw new IllegalPathException();
	}

	/**
	 *
	 * @param propertyView
	 */
	public void configurePropertyView(Value<String> propertyView) {}

	public void configureIdProperty(String idProperty) {
		this.idProperty = idProperty;
	}

	// ----- protected methods -----
	protected DirectedRelationship findDirectedRelationship(TypedIdConstraint constraint1, TypeConstraint constraint2) {
		return findDirectedRelationship(constraint1.getTypeConstraint(), constraint2);
	}

	protected DirectedRelationship findDirectedRelationship(TypeConstraint constraint1, TypedIdConstraint constraint2) {
		return findDirectedRelationship(constraint1, constraint2.getTypeConstraint());
	}

	protected DirectedRelationship findDirectedRelationship(TypedIdConstraint constraint1, TypedIdConstraint constraint2) {
		return findDirectedRelationship(constraint1.getTypeConstraint(), constraint2.getTypeConstraint());
	}

	protected DirectedRelationship findDirectedRelationship(TypeConstraint constraint1, TypeConstraint constraint2) {

		String type1             = constraint1.getRawType();
		String type2             = constraint2.getRawType();

		// try raw type first..
		return EntityContext.getDirectedRelationship(type1, type2);

	}

	protected boolean notifyOfTraversal(List<GraphObject> traversedNodes, ErrorBuffer errorBuffer) {

		boolean hasError = false;
		for (VetoableGraphObjectListener listener : EntityContext.getModificationListeners()) {

			hasError |= listener.wasVisited(traversedNodes, -1, errorBuffer, securityContext);

		}
		return hasError;
	}

	protected String buildLocationHeader(GraphObject newObject) {

		StringBuilder uriBuilder = securityContext.getBaseURI();

		uriBuilder.append(getUriPart());
		uriBuilder.append("/");

		if (newObject != null) {

			// use configured id property
			if (idProperty == null) {

				uriBuilder.append(newObject.getId());

			} else {

				uriBuilder.append(newObject.getProperty(idProperty));

			}
		}

		return uriBuilder.toString();
	}

	//~--- get methods ----------------------------------------------------

	public abstract String getUriPart();

	public abstract boolean isCollectionResource();

	//~--- set methods ----------------------------------------------------

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}
}
