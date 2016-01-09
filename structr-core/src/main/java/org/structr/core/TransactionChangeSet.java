/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

/**
 * Encapsulates the changeset of a neo4j database transaction, taking care of
 * structr internals.
 * 
 *
 */
public class TransactionChangeSet {

	private Queue<AbstractNode> propagationQueue    = new ConcurrentLinkedQueue<AbstractNode>();
	private boolean systemOnly                      = true;
	
	private Queue<AbstractRelationship> modifiedRels  = new ConcurrentLinkedQueue<AbstractRelationship>();
	private Queue<AbstractRelationship> createdRels   = new ConcurrentLinkedQueue<AbstractRelationship>();
	private Queue<AbstractRelationship> deletedRels   = new ConcurrentLinkedQueue<AbstractRelationship>();

	private Queue<AbstractNode> modifiedNodes         = new ConcurrentLinkedQueue<AbstractNode>();
	private Queue<AbstractNode> createdNodes          = new ConcurrentLinkedQueue<AbstractNode>();
	private Queue<AbstractNode> deletedNodes          = new ConcurrentLinkedQueue<AbstractNode>();

	private Queue<AbstractNode> ownerModifiedNodes    = new ConcurrentLinkedQueue<AbstractNode>();
	private Queue<AbstractNode> securityModifiedNodes = new ConcurrentLinkedQueue<AbstractNode>();
	private Queue<AbstractNode> locationModifiedNodes = new ConcurrentLinkedQueue<AbstractNode>();

	public void include(TransactionChangeSet changeSet) {
		
		modifiedRels.addAll(changeSet.getModifiedRelationships());
		createdRels.addAll(changeSet.getCreatedRelationships());
		deletedRels.addAll(changeSet.getDeletedRelationships());
		
		modifiedNodes.addAll(changeSet.getModifiedNodes());
		createdNodes.addAll(changeSet.getCreatedNodes());
		deletedNodes.addAll(changeSet.getDeletedNodes());
		
		ownerModifiedNodes.addAll(changeSet.getOwnerModifiedNodes());
		securityModifiedNodes.addAll(changeSet.getSecurityModifiedNodes());
		locationModifiedNodes.addAll(changeSet.getLocationModifiedNodes());
		
		// remove deleted node from other transactions when merging
		ownerModifiedNodes.removeAll(deletedNodes);
		securityModifiedNodes.removeAll(deletedNodes);
		locationModifiedNodes.removeAll(deletedNodes);
		
		modifiedNodes.removeAll(deletedNodes);
		createdNodes.removeAll(deletedNodes);
		
		// remove deleted relationships from other transactions when merging
		modifiedRels.removeAll(deletedRels);
		createdRels.removeAll(deletedRels);
		
		propagationQueue.addAll(changeSet.getPropagationQueue());
	}
	
	public void clear() {
		modifiedRels.clear();
		createdRels.clear();
		deletedRels.clear();
		
		modifiedNodes.clear();
		createdNodes.clear();
		deletedNodes.clear();
		
		ownerModifiedNodes.clear();
		securityModifiedNodes.clear();
		locationModifiedNodes.clear();
		
		propagationQueue.clear();
	}

	@Override
	public String toString() {
		
		StringBuilder buf = new StringBuilder();
		
		buf.append("Nodes: ");
		buf.append(createdNodes.size());
		buf.append("/");
		buf.append(modifiedNodes.size());
		buf.append("/");
		buf.append(deletedNodes.size());
		buf.append(", ");
		buf.append("Rels: ");
		buf.append(createdRels.size());
		buf.append("/");
		buf.append(modifiedRels.size());
		buf.append("/");
		buf.append(deletedRels.size());
		
		if (systemOnly) {
			buf.append(" system only");
		}

		return buf.toString();
	}
	
	public void nonSystemProperty() {
		systemOnly = false;
	}
	
	public boolean systemOnly() {
		return systemOnly;
	}
	
	public void create(AbstractNode created) {

		createdNodes.add(created);
		propagationQueue.add(created);
		
		systemOnly = false;
	}

	public void modify(AbstractNode modified) {
		
		if (!isNewOrDeleted(modified)) {
			
			modifiedNodes.add(modified);
			propagationQueue.add(modified);
		}
	}
	
	public void delete(AbstractNode deleted) {
		
		propagationQueue.remove(deleted);
		createdNodes.remove(deleted);
		modifiedNodes.remove(deleted);
		ownerModifiedNodes.remove(deleted);
		securityModifiedNodes.remove(deleted);
		locationModifiedNodes.remove(deleted);

		deletedNodes.add(deleted);
		
		systemOnly = false;
	}
	
	public void create(AbstractRelationship created) {
			
		createdRels.add(created);
		systemOnly = false;
	}

	public void modify(AbstractRelationship modified) {
		
		if (!isNewOrDeleted(modified)) {
			
			modifiedRels.add(modified);
		}
	}
	
	public void delete(AbstractRelationship deleted) {
		
		createdRels.remove(deleted);
		modifiedRels.remove(deleted);
		
		deletedRels.add(deleted);
		
		systemOnly = false;
	}
	
	public void modifyRelationshipEndpoint(AbstractNode node, RelationshipType relationshipType) {
		
		if (relationshipType instanceof RelType) {
		
			switch ((RelType) relationshipType) {

				case OWNS:
					modifyOwner(node);
					break;

				case SECURITY:
					modifySecurity(node);
					break;

				case IS_AT:
					modifyLocation(node);
					break;

				default:
					modify(node);
					break;
			}
			
		} else {
			
			modify(node);
			
		}
	}
	
	public void modifyOwner(AbstractNode ownerModified) {
		
		if (!isNewOrDeleted(ownerModified)) {
			
			ownerModifiedNodes.add(ownerModified);
			propagationQueue.add(ownerModified);
		}
	}
	
	public void modifySecurity(AbstractNode securityModified) {
		
		if (!isNewOrDeleted(securityModified)) {
			
			securityModifiedNodes.add(securityModified);
			propagationQueue.add(securityModified);
		}
	}
	
	public void modifyLocation(AbstractNode locationModified) {
		
		if (!isNewOrDeleted(locationModified)) {
			
			locationModifiedNodes.add(locationModified);
			propagationQueue.add(locationModified);
		}
	}
	
	public Queue<AbstractRelationship> getModifiedRelationships() {
		return modifiedRels;
	}

	public Queue<AbstractRelationship> getCreatedRelationships() {
		return createdRels;
	}

	public Queue<AbstractRelationship> getDeletedRelationships() {
		return deletedRels;
	}

	public Queue<AbstractNode> getModifiedNodes() {
		return modifiedNodes;
	}

	public Queue<AbstractNode> getCreatedNodes() {
		return createdNodes;
	}

	public Queue<AbstractNode> getDeletedNodes() {
		return deletedNodes;
	}

	public Queue<AbstractNode> getOwnerModifiedNodes() {
		return ownerModifiedNodes;
	}

	public Queue<AbstractNode> getSecurityModifiedNodes() {
		return securityModifiedNodes;
	}

	public Queue<AbstractNode> getLocationModifiedNodes() {
		return locationModifiedNodes;
	}
	
	public Queue<AbstractNode> getPropagationQueue() {
		return propagationQueue;
	}

	// ----- private methods -----
	private boolean isNew(AbstractNode node) {
		return createdNodes.contains(node);
	}
	
	private boolean isNew(AbstractRelationship relationship) {
		return createdRels.contains(relationship);
	}
	
	private boolean isDeleted(AbstractNode node) {
		return deletedNodes.contains(node);
	}
	
	private boolean isDeleted(AbstractRelationship relationship) {
		return deletedRels.contains(relationship);
	}
	
	private boolean isNewOrDeleted(AbstractNode node) {
		return isNew(node) || isDeleted(node);
	}
	
	private boolean isNewOrDeleted(AbstractRelationship relationship) {
		return isNew(relationship) || isDeleted(relationship);
	}
}
