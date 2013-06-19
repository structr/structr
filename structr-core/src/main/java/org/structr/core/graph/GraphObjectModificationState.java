package org.structr.core.graph;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
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

	private static final Logger logger = Logger.getLogger(GraphObjectModificationState.class.getName());
	
	private static final int STATE_DELETED =                   1;
	private static final int STATE_MODIFIED =                  2;
	private static final int STATE_CREATED =                   4;
	private static final int STATE_DELETED_PASSIVELY =         8;
	private static final int STATE_OWNER_MODIFIED =           16;
	private static final int STATE_SECURITY_MODIFIED =        32;
	private static final int STATE_LOCATION_MODIFIED =        64;
	private static final int STATE_PROPAGATED_MODIFICATION = 128;
	
	private PropertyMap removedProperties = new PropertyMap();
	private boolean modified              = false;
	private GraphObject object            = null;
	private int status                    = 0;

	public GraphObjectModificationState(GraphObject object) {
		this.object = object;
	}

	@Override
	public String toString() {
		return object.getClass().getSimpleName() + "(" + object + "); " + status;
	}

	public void propagatedModification() {
		
		int statusBefore = status;
		
		status |= STATE_PROPAGATED_MODIFICATION;
		
		if (status != statusBefore) {
			modified = true;
		}
	}

	public void modifyLocation() {
		
		int statusBefore = status;
		
		status |= STATE_LOCATION_MODIFIED | STATE_PROPAGATED_MODIFICATION;
		
		if (status != statusBefore) {
			modified = true;
		}
	}
	
	public void modifySecurity() {
		
		int statusBefore = status;
		
		status |= STATE_SECURITY_MODIFIED | STATE_PROPAGATED_MODIFICATION;
		
		if (status != statusBefore) {
			modified = true;
		}
	}
	
	public void modifyOwner() {
		
		int statusBefore = status;
		
		status |= STATE_OWNER_MODIFIED | STATE_PROPAGATED_MODIFICATION;
		
		if (status != statusBefore) {
			modified = true;
		}
	}
	
	public void create() {
		
		int statusBefore = status;
		
		status |= STATE_CREATED | STATE_PROPAGATED_MODIFICATION;
		
		if (status != statusBefore) {
			modified = true;
		}
	}

	public void modify(PropertyKey key, Object previousValue) {
		
		int statusBefore = status;
		
		status |= STATE_MODIFIED | STATE_PROPAGATED_MODIFICATION;

		// store previous value
		if (key != null) {
			removedProperties.put(key, previousValue);
		}
		
		if (status != statusBefore) {
			modified = true;
		}
	}

	public void delete(boolean passive) {
		
		int statusBefore = status;
		
		if (passive) {
			status |= STATE_DELETED_PASSIVELY;
		}

		status |= STATE_DELETED;
		
		if (status != statusBefore) {
			modified = true;
		}
	}

	public boolean isPassivelyDeleted() {
		return (status & STATE_DELETED_PASSIVELY) == STATE_DELETED_PASSIVELY;
	}

	public boolean isCreated() {
		return (status & STATE_CREATED) == STATE_CREATED;
	}

	public boolean isModified() {
		return (status & STATE_MODIFIED) == STATE_MODIFIED;
	}

	public boolean isDeleted() {
		return (status & STATE_DELETED) == STATE_DELETED;
	}

	/**
	 * Call beforeModification/Creation/Deletion methods.
	 * 
	 * @param securityContext
	 * @param errorBuffer
	 * @return
	 * @throws FrameworkException 
	 */
	public boolean doInnerCallback(ModificationQueue modificationQueue, SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		boolean valid = true;
	
		// check for modification propagation along the relationships
		if ((status & STATE_PROPAGATED_MODIFICATION) == STATE_PROPAGATED_MODIFICATION && object instanceof AbstractNode) {
			
			Set<AbstractNode> nodes = ((AbstractNode)object).getNodesForModificationPropagation();
			if (nodes != null) {

				for (AbstractNode node : nodes) {

					modificationQueue.propagatedModification(node);
				}
			}

		}
		
		// examine only the last 4 bits here
		switch (status & 0x000f) {

			case 15:
			case 14:
			case 13:
			case 12:
			case 11:
			case 10:
			case  9:
			case  8: // since all values >= 8 mean that the object was passively deleted, no action has to be taken
				 // (no callback for passive deletion!)
				break;

			case 7:	// created, modified, deleted, poor guy => no callback
				break;

			case 6: // created, modified => only creation callback will be called
				valid &= object.onCreation(securityContext, errorBuffer);
				break;

			case 5: // created, deleted => no callback
				break;

			case 4: // created => creation callback
				valid &= object.onCreation(securityContext, errorBuffer);
				break;

			case 3: // modified, deleted => deletion callback
				valid &= object.onDeletion(securityContext, errorBuffer, removedProperties);
				break;

			case 2: // modified => modification callback
				valid &= object.onModification(securityContext, errorBuffer);
				break;

			case 1: // deleted => deletion callback
				valid &= object.onDeletion(securityContext, errorBuffer, removedProperties);
				break;

			case 0:	// no action, no callback
				break;

			default:
				break;
		}

		// mark as finished
		modified = false;
		
		return valid;
	}

	/**
	 * Call beforeModification/Creation/Deletion methods.
	 * 
	 * @param securityContext
	 * @param errorBuffer
	 * @return
	 * @throws FrameworkException 
	 */
	public boolean doValidationAndIndexing(ModificationQueue modificationQueue, SecurityContext securityContext, ErrorBuffer errorBuffer, boolean doValidation) throws FrameworkException {

		boolean valid = true;
	
		// examine only the last 4 bits here
		switch (status & 0x000f) {

			case 6: // created, modified => only creation callback will be called
				if (doValidation) {
					valid &= validate(securityContext, errorBuffer);
				}
				addToIndex();
				break;

			case 4: // created => creation callback
				if (doValidation) {
					valid &= validate(securityContext, errorBuffer);
				}
				addToIndex();
				break;

			case 2: // modified => modification callback
				if (doValidation) {
					valid &= validate(securityContext, errorBuffer);
				}
				updateInIndex();
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

		if ((status & (STATE_DELETED | STATE_DELETED_PASSIVELY)) == 0) {

			if ((status & STATE_PROPAGATED_MODIFICATION) == STATE_PROPAGATED_MODIFICATION) {
				object.propagatedModification(securityContext);
			}

			if ((status & STATE_LOCATION_MODIFIED) == STATE_LOCATION_MODIFIED) {
				object.locationModified(securityContext);
			}

			if ((status & STATE_SECURITY_MODIFIED) == STATE_SECURITY_MODIFIED) {
				object.securityModified(securityContext);
			}

			if ((status & STATE_OWNER_MODIFIED) == STATE_OWNER_MODIFIED) {
				object.ownerModified(securityContext);
			}
		}
		
		// examine only the last 4 bits here
		switch (status & 0x000f) {

			case 15:
			case 14:
			case 13:
			case 12:
			case 11:
			case 10:
			case  9:
			case  8: // since all values >= 8 mean that the object was passively deleted, no action has to be taken
				 // (no callback for passive deletion!)
				break;

			case  7: // created, modified, deleted, poor guy => no callback
				break;

			case  6: // created, modified => only creation callback will be called
				object.afterCreation(securityContext);
				break;

			case  5: // created, deleted => no callback
				break;

			case  4: // created => creation callback
				object.afterCreation(securityContext);
				break;

			case  3: // modified, deleted => no callback as node/rel is gone
				break;

			case  2: // modified => modification callback
				object.afterModification(securityContext);
				break;

			case  1: // deleted => no callback as node/rel is gone
				break;

			case  0: // no action, no callback
				break;

			default:
				break;
		}
	}

	public GraphObject getObject() {
		return object;
	}
	
	public boolean wasModified() {
		return modified;
	}
	
	/**
	 * Call validators. This must be synchronized globally
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
