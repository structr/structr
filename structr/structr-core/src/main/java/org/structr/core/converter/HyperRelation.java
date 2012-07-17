/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.converter;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;
import org.structr.common.PropertyKey;
import org.structr.common.SecurityContext;
import org.structr.core.Value;
import org.structr.core.notion.Notion;

/**
 *
 * @author Christian Morgner
 */
public class HyperRelation implements Value<HyperRelation> {
	
	private PropertyKey parentIdKey		= null;
	private PropertyKey childIdKey		= null;
	private PropertyKey refIdKey		= null;
	private Direction direction		= null;
	private RelationshipType relType	= null;
	private Notion notion			= null;
	private Class entity			= null;
	
	public HyperRelation(Class entity, RelationshipType relType, Direction direction, Notion notion) {
		
		this.entity = entity;
		this.relType = relType;
		this.direction = direction;
		this.notion = notion;
	}

	public PropertyKey getParentIdKey() {
		return parentIdKey;
	}

	public PropertyKey getChildIdKey() {
		return childIdKey;
	}

	public PropertyKey getRefIdKey() {
		return refIdKey;
	}

	// ----- interface value -----
	@Override
	public void set(SecurityContext securityContext, HyperRelation value) {
	}

	@Override
	public HyperRelation get(SecurityContext securityContext) {
		return this;
	}

	public Direction getDirection() {
		return direction;
	}

	public RelationshipType getRelType() {
		return relType;
	}

	public Class getEntity() {
		return entity;
	}

	public Notion getNotion() {
		return notion;
	}
}
