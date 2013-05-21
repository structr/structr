package org.structr.core.graph;

import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import org.neo4j.graphdb.RelationshipType;
import static org.structr.common.RelType.IS_AT;
import static org.structr.common.RelType.OWNS;
import static org.structr.common.RelType.SECURITY;
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

	private ConcurrentSkipListMap<String, GraphObjectModificationState> modifications = new ConcurrentSkipListMap<String, GraphObjectModificationState>();
	private Set<String> alreadyPropagated                                             = new LinkedHashSet<String>();
	
	public boolean doInnerCallbacks(SecurityContext securityContext, ErrorBuffer errorBuffer, boolean doValidation) throws FrameworkException {

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
	}

	public void create(AbstractRelationship relationship) {

		getState(relationship).create();

		modifyEndNodes(relationship.getStartNode(), relationship.getEndNode(), relationship.getRelType());
	}

	public void modifyOwner(AbstractNode node) {
		getState(node).modifyOwner();
	}
	
	public void modifySecurity(AbstractNode node) {
		getState(node).modifySecurity();
	}
	
	public void modifyLocation(AbstractNode node) {
		getState(node).modifyLocation();
	}
	
	public void modify(AbstractNode node, PropertyKey key, Object previousValue) {
		getState(node).modify(key, previousValue);
	}

	public void modify(AbstractRelationship relationship, PropertyKey key, Object previousValue) {
		getState(relationship).modify(key, previousValue);
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

		if (OWNS.equals(relType)) {

			modifyOwner(startNode);
			modifyOwner(endNode);
			return;
		}

		if (SECURITY.equals(relType)) {

			modifySecurity(startNode);
			modifySecurity(endNode);
			return;
		}

		if (IS_AT.equals(relType)) {

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
