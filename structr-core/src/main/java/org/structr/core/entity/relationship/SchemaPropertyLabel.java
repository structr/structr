package org.structr.core.entity.relationship;

import org.structr.core.entity.ManyToMany;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaLabel;
import org.structr.core.entity.SchemaProperty;

/**
 *
 * @author Christian Morgner
 */
public class SchemaPropertyLabel extends ManyToMany<SchemaProperty, SchemaLabel> {

	@Override
	public Class<SchemaProperty> getSourceType() {
		return SchemaProperty.class;
	}

	@Override
	public Class<SchemaLabel> getTargetType() {
		return SchemaLabel.class;
	}

	@Override
	public String name() {
		return "LABELS";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return NONE;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
