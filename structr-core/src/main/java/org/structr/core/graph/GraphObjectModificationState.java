package org.structr.core.graph;

import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

/**
 *
 * @author Christian Morgner
 */
public class GraphObjectModificationState {

	private PropertyMap removedProperties = new PropertyMap();
	private GraphObject object            = null;
	private int status                    = 0;

	public GraphObjectModificationState(GraphObject object) {
		this.object = object;
	}

	@Override
	public String toString() {
		return object.getClass().getSimpleName() + "(" + object + "); " + status;
	}

	public void create() {
		status |= 4;
	}

	public void modify(PropertyKey key, Object previousValue) {

		status |= 2;

		// store previous value
		if (key != null) {
			removedProperties.put(key, previousValue);
		}
	}

	public void delete(boolean passive) {

		if (passive) {
			status |= 8;
		}

		status |= 1;
	}

	public boolean isCreated() {
		return (status & 4) == 4;
	}

	public boolean isModified() {
		return (status & 2) == 2;
	}

	public boolean isDeleted() {
		return (status & 1) == 1;
	}

	public boolean isPassivelyDeleted() {
		return (status & 8) == 8;
	}

	/**
	 * Call beforeModification/Creation/Deletion methods.
	 * 
	 * @param securityContext
	 * @param errorBuffer
	 * @return
	 * @throws FrameworkException 
	 */
	public boolean doInnerCallback(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		boolean valid = true;

		switch (status) {

			case 9: // deleted, and more specificly: passively deleted, i.e. this
				// relationship was deleted because a node was deleted in the
				// first place. This needs to be handled differently, i.e. no
				// deletion callback may be called!
				break;

			case 8:	// passively deleted, won't happen
				break;

			case 7:	// created, modified, deleted, poor guy => no callback
				break;

			case 6: // created, modified => only creation callback will be called
				valid &= validate(securityContext, errorBuffer);
				valid &= object.beforeCreation(securityContext, errorBuffer);
				addToIndex();
				break;

			case 5: // created, deleted => no callback
				break;

			case 4: // created => creation callback
				valid &= validate(securityContext, errorBuffer);
				valid &= object.beforeCreation(securityContext, errorBuffer);
				addToIndex();
				break;

			case 3: // modified, deleted => deletion callback
				valid &= object.beforeDeletion(securityContext, errorBuffer, removedProperties);
				break;

			case 2: // modified => modification callback
				valid &= validate(securityContext, errorBuffer);
				valid &= object.beforeModification(securityContext, errorBuffer);
				updateInIndex();
				break;

			case 1: // deleted => deletion callback
				valid &= object.beforeDeletion(securityContext, errorBuffer, removedProperties);
				break;

			case 0:	// no action, no callback
				break;

			default:
				break;
		}

		return valid;
	}

	/**
	 * Call afterModification/Creation/Deletion methods.
	 * 
	 * @param securityContext 
	 */
	public void doOuterCallback(SecurityContext securityContext) {

		switch (status) {

			case 9: // deleted, and more specificly: passively deleted, i.e. this
				// relationship was deleted because a node was deleted in the
				// first place. This needs to be handled differently, i.e. no
				// deletion callback may be called!
				break;

			case 8:	// passively deleted, won't happen
				break;

			case 7:	// created, modified, deleted, poor guy => no callback
				break;

			case 6: // created, modified => only creation callback will be called
				object.afterCreation(securityContext);
				break;

			case 5: // created, deleted => no callback
				break;

			case 4: // created => creation callback
				object.afterCreation(securityContext);
				break;

			case 3: // modified, deleted => deletion callback
				object.afterDeletion(securityContext);
				break;

			case 2: // modified => modification callback
				object.afterModification(securityContext);
				break;

			case 1: // deleted => deletion callback
				object.afterDeletion(securityContext);
				break;

			case 0:	// no action, no callback
				break;

			default:
				break;
		}
	}

	public GraphObject getObject() {
		return object;
	}
	
	/**
	 * Call validators.
	 * 
	 * @param securityContext
	 * @param errorBuffer
	 * @return 
	 */
	private boolean validate(SecurityContext securityContext, ErrorBuffer errorBuffer) {
		
		boolean valid = true;
				
		for (PropertyKey key : removedProperties.keySet()) {

			List<PropertyValidator> validators = key.getValidators();
			for (PropertyValidator validator : validators) {
				
				Object value = object.getProperty(key);
				
				valid &= validator.isValid(securityContext, object, key, value, errorBuffer);
			}
		}

		return valid;
	}

	private void addToIndex() throws FrameworkException {

		if (object instanceof AbstractNode) {

			Services.command(SecurityContext.getSuperUserInstance(), NewIndexNodeCommand.class).addNode((AbstractNode)object);

		} else if (object instanceof AbstractRelationship) {

			Services.command(SecurityContext.getSuperUserInstance(), IndexRelationshipCommand.class).execute((AbstractRelationship)object);
		}
	}

	private void updateInIndex() throws FrameworkException {

		if (object instanceof AbstractNode) {

			Services.command(SecurityContext.getSuperUserInstance(), NewIndexNodeCommand.class).updateNode((AbstractNode)object);

		} else if (object instanceof AbstractRelationship) {

			Services.command(SecurityContext.getSuperUserInstance(), IndexRelationshipCommand.class).execute((AbstractRelationship)object);
		}
	}
}
