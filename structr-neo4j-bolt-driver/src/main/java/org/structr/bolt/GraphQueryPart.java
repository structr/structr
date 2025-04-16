/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.bolt;

import org.structr.api.graph.Direction;
import org.structr.api.search.GraphQuery;
import org.structr.api.search.Operation;

import java.util.Set;

/**
 */
public class GraphQueryPart {

	private Operation operation = null;
	private Set<Object> values  = null;
	private Direction direction = null;
	private String label        = null;
	private String otherLabel   = null;
	private String relationship = null;
	private String identifier   = null;

	public GraphQueryPart(final GraphQuery query) {

		this.values       = query.getValues();
		this.label        = query.getLabel();
		this.otherLabel   = query.getOtherLabel();
		this.direction    = query.getDirection();
		this.relationship = query.getRelationship();
	}

	public String getLinkIdentifier() {
		return "(" + label + ")" + getRelationshipPattern() + "(" + otherLabel + ")";
	}

	public Operation getOperation() {
		return operation;
	}

	public Set<Object> getValues() {
		return values;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public String getOtherLabel() {
		return otherLabel;
	}

	public void setOtherLabel(final String otherLabel) {
		this.otherLabel = otherLabel;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}

	public String getRelationship() {
		return relationship;
	}

	public void setRelationship(final String relationship) {
		this.relationship = relationship;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(final Direction direction) {
		this.direction = direction;
	}

	public String getRelationshipPattern() {

		final StringBuilder buf  = new StringBuilder();

		switch (direction) {

			case INCOMING:
				buf.append("<-");
				break;

			case BOTH:
			case OUTGOING:
				buf.append("-");
				break;
		}

		buf.append("[:");
		buf.append(relationship);
		buf.append("]");

		switch (direction) {

			case BOTH:
			case INCOMING:
				buf.append("-");
				break;

			case OUTGOING:
				buf.append("->");
				break;
		}

		return buf.toString();
	}

}
