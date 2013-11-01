package org.structr.web.entity.html.relation;

import org.neo4j.graphdb.RelationshipType;
import org.structr.common.RelType;
import org.structr.core.entity.ManyToMany;
import org.structr.web.entity.html.Form;
import org.structr.web.entity.html.Input;

/**
 *
 * @author Christian Morgner
 */
public class FormInput extends ManyToMany<Form, Input> {

	@Override
	public Class<Form> getSourceType() {
		return Form.class;
	}

	@Override
	public Class<Input> getTargetType() {
		return Input.class;
	}

	@Override
	public RelationshipType getRelationshipType() {
		return RelType.CONTAINS;
	}
}
