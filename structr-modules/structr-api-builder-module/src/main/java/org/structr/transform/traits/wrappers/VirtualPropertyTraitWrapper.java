/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.transform.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.transform.Transformation;
import org.structr.transform.VirtualProperty;
import org.structr.transform.traits.definitions.VirtualPropertyTraitDefinition;

public class VirtualPropertyTraitWrapper extends AbstractNodeTraitWrapper implements VirtualProperty {

	public VirtualPropertyTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public Integer getPosition() {
		return wrappedObject.getProperty(traits.key(VirtualPropertyTraitDefinition.POSITION_PROPERTY));
	}

	@Override
	public String getSourceName() {
		return wrappedObject.getProperty(traits.key(VirtualPropertyTraitDefinition.SOURCE_NAME_PROPERTY));
	}

	@Override
	public String getTargetName() {
		return wrappedObject.getProperty(traits.key(VirtualPropertyTraitDefinition.TARGET_NAME_PROPERTY));
	}

	@Override
	public String getOutputFunction() {
		return wrappedObject.getProperty(traits.key(VirtualPropertyTraitDefinition.OUTPUT_FUNCTION_PROPERTY));
	}

	@Override
	public String getInputFunction() {
		return wrappedObject.getProperty(traits.key(VirtualPropertyTraitDefinition.INPUT_FUNCTION_PROPERTY));
	}

	@Override
	public Transformation getTransformation(final String _type) throws FrameworkException {

		final String _sourceName     = getSourceName();
		final String _inputFunction  = getInputFunction();
		final String _outputFunction = getOutputFunction();
		String _targetName           = getTargetName();

		if (_sourceName == null && _outputFunction == null) {
			throw new FrameworkException(500, "VirtualProperty with ID " + getUuid() + " needs source name or output function");
		}

		// don't rename
		if (_targetName == null) {
			_targetName = _sourceName;
		}

		return new Transformation(_type, _sourceName, _targetName, _inputFunction, _outputFunction);
	}
}
