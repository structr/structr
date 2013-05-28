package org.structr.core.graph;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
	
public class ModificationQueue {

	private static final Logger logger = Logger.getLogger(ModificationQueue.class.getName());
	
	private ConcurrentSkipListMap<String, GraphObjectModificationState> modifications = new ConcurrentSkipListMap<String, GraphObjectModificationState>();
	private Set<String> alreadyPropagated                                             = new LinkedHashSet<String>();
	private Set<String> types                                                         = new LinkedHashSet<String>();
	
	/**
	 * Returns a set containing the different entity types of
	 * nodes modified in this queue.
	 * 
	 * @return the types
	 */
	public Set<String> getTypes() {
		return types;
	}
	
	public boolean doInnerCallbacks(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		boolean hasModifications = true;
		boolean valid = true;

		// collect all modified nodes
		while (hasModifications) {

			hasModifications = false;

			for (GraphObjectModificationState state : modifications.values()) {

				if (state.wasModified()) {

					// do callback according to entry state
					valid &= state.doInnerCallback(this, securityContext, errorBuffer);
					hasModifications = true;
				}
			}
		}

		return valid;
	}
	
	public boolean doValidation(SecurityContext securityContext, ErrorBuffer errorBuffer, boolean doValidation) throws FrameworkException {
		
		boolean valid = true;
		
		// do validation and indexing
		for (Entry<String, GraphObjectModificationState> entry : modifications.entrySet()) {

			// do callback according to entry state
			valid &= entry.getValue().doValidationAndIndexing(this, securityContext, errorBuffer, doValidation);
		}
		
		return valid;
	}

	public void doOuterCallbacksAndCleanup(SecurityContext securityContext) {

		// copy modifications, do after transaction callbacks
		for (GraphObjectModificationState state : modifications.values()) {

			if (!state.isDeleted()) {
				state.doOuterCallback(securityContext);
			}
		}

		// clear collections afterwards
		alreadyPropagated.clear();
		modifications.clear();
	}

	public void create(AbstractNode node) {
		getState(node).create();
		types.add(node.getType());
	}

	public void create(AbstractRelationship relationship) {

		getState(relationship).create();

		modifyEndNodes(relationship.getStartNode(), relationship.getEndNode(), relationship.getRelType());
		
		types.add(relationship.getType());
	}

	public void modifyOwner(AbstractNode node) {
		getState(node).modifyOwner();
		types.add(node.getType());
	}
	
	public void modifySecurity(AbstractNode node) {
		getState(node).modifySecurity();
		types.add(node.getType());
	}
	
	public void modifyLocation(AbstractNode node) {
		getState(node).modifyLocation();
		types.add(node.getType());
	}
	
	public void modify(AbstractNode node, PropertyKey key, Object previousValue) {
		getState(node).modify(key, previousValue);
		types.add(node.getType());
	}

	public void modify(AbstractRelationship relationship, PropertyKey key, Object previousValue) {
		getState(relationship).modify(key, previousValue);
		types.add(relationship.getType());
	}
	
	public void propagatedModification(AbstractNode node) {
		
		GraphObjectModificationState state = getState(node, true);
		if (state != null) {
			
			state.propagatedModification();

			// save hash to avoid repeated propagation
			alreadyPropagated.add(hash(node));
		}
	}

	public void delete(AbstractNode node) {
		getState(node).delete(false);
	}

	public void delete(AbstractRelationship relationship, boolean passive) {

		getState(relationship).delete(passive);

		modifyEndNodes(relationship.getStartNode(), relationship.getEndNode(), relationship.getRelType());
	}

	private void modifyEndNodes(AbstractNode startNode, AbstractNode endNode, RelationshipType relType) {

		if (RelType.OWNS.equals(relType)) {

			modifyOwner(startNode);
			modifyOwner(endNode);
			return;
		}

		if (RelType.SECURITY.equals(relType)) {

			modifySecurity(startNode);
			modifySecurity(endNode);
			return;
		}

		if (RelType.IS_AT.equals(relType)) {

			modifyLocation(startNode);
			modifyLocation(endNode);
			return;
		}

		modify(startNode, null, null);
		modify(endNode, null, null);
	}

	private GraphObjectModificationState getState(AbstractNode node) {
		return getState(node, false);
	}
	
	private GraphObjectModificationState getState(AbstractNode node, boolean checkPropagation) {

		String hash = hash(node);
		GraphObjectModificationState state = modifications.get(hash);

		if (state == null && !(checkPropagation && alreadyPropagated.contains(hash))) {

			state = new GraphObjectModificationState(node);
			modifications.put(hash, state);
		}

		return state;
	}

	private GraphObjectModificationState getState(AbstractRelationship rel) {

		String hash = hash(rel);
		GraphObjectModificationState state = modifications.get(hash);

		if (state == null) {

			state = new GraphObjectModificationState(rel);
			modifications.put(hash, state);
		}

		return state;
	}

	private String hash(AbstractNode node) {
		return "N" + node.getId();
	}

	private String hash(AbstractRelationship rel) {
		return "R" + rel.getId();
	}
}
