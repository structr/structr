package org.structr.core.entity.relationship;

import org.structr.core.entity.ManyToMany;
import org.structr.core.entity.Transformation;

/**
 *
 * @author Christian Morgner
 */
public class Transform extends ManyToMany<Transformation, Transformation> {

	@Override
	public Class<Transformation> getSourceType() {
		return Transformation.class;
	}

	@Override
	public Class<Transformation> getTargetType() {
		return Transformation.class;
	}

	@Override
	public String name() {
		return "TRANSFORM";
	}
}
