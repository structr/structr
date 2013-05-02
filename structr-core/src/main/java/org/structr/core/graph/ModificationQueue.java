package org.structr.core.graph;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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

	private static final Logger logger = Logger.getLogger(ModificationQueue.class.getName());
	
	private ConcurrentSkipListMap<String, GraphObjectModificationState> modifications = new ConcurrentSkipListMap<String, GraphObjectModificationState>();
	private Map<String, GraphObjectModificationState> immutableState                  = new LinkedHashMap<String, GraphObjectModificationState>();
	
	public boolean doInnerCallbacks(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		boolean valid = true;
		int count     = 0;

		while (!modifications.isEmpty()) {

			Map.Entry<String, GraphObjectModificationState> entry = modifications.pollFirstEntry();
			if (entry != null) {

				// do callback according to entry state
				valid &= entry.getValue().doInnerCallback(securityContext, errorBuffer);

				// store entries for later notification
				if (!immutableState.containsKey(entry.getKey())) {
					immutableState.put(entry.getKey(), entry.getValue());
				}
			}
			
			if (count++ > 10000) {
				logger.log(Level.WARNING, "################################################################################### Too many modification callbacks!");
			}
		}

		return valid;
	}

	public void doOuterCallbacks(SecurityContext securityContext) {

		// copy modifications, do after transaction callbacks
		for (GraphObjectModificationState state : immutableState.values()) {

			if (!state.isDeleted()) {
				state.doOuterCallback(securityContext);
			}
		}

		// clear map afterwards
		immutableState.clear();
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

		String hash = hash(node);
		GraphObjectModificationState state = modifications.get(hash);

		if (state == null) {

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
