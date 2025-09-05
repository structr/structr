/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.transform;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.util.Map;

/**
 *
 */
public class Transformation {

	private String sourceName          = null;
	private String targetName          = null;
	private String inputFunction       = null;
	private String outputFunction      = null;
	private PropertyKey sourceProperty = null;
	private PropertyKey targetProperty = null;

	public Transformation(final String type, final String sourceName, final String targetName, final String inputFunction, final String outputFunction) {

		this.sourceName     = sourceName;
		this.targetName     = targetName;
		this.inputFunction  = inputFunction;
		this.outputFunction = outputFunction;

		final Traits traits = Traits.of(type);
		this.sourceProperty = traits.key(sourceName);
		this.targetProperty = new GenericProperty(targetName);
	}

	public Object transformOutput(final ActionContext actionContext, final GraphObject source) throws FrameworkException {

		if (outputFunction == null) {
			return source.getProperty(sourceProperty);
		}

		// output transformation requested
		actionContext.setConstant("input", source);
		return Scripting.evaluate(actionContext, null, "${" + outputFunction.trim() + "}", "virtual property " + targetName, null);
	}

	public void transformInput(final ActionContext actionContext, final Map<String, Object> source) throws FrameworkException {

		if (source != null) {

			// move / rename input value
			Object inputValue = source.remove(targetName);
			if (inputValue != null) {

				if (inputFunction != null) {

					// input transformation requested
					actionContext.setConstant("input", inputValue);
					inputValue = Scripting.evaluate(actionContext, null, "${" + inputFunction.trim() + "}", " virtual property " + sourceName, null);
				}

				source.put(sourceName, inputValue);
			}
		}
	}

	public String getSourceName() {
		return sourceName;
	}

	public String getTargetName() {
		return targetName;
	}

	public PropertyKey getSourceProperty() {
		return sourceProperty;
	}

	public PropertyKey getTargetProperty() {
		return targetProperty;
	}
}
