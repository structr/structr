package org.structr.rest.transform;

import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;

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

	public Transformation(final Class type, final String sourceName, final String targetName, final String inputFunction, final String outputFunction) {

		this.sourceName     = sourceName;
		this.targetName     = targetName;
		this.inputFunction  = inputFunction;
		this.outputFunction = outputFunction;

		this.sourceProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(type, sourceName);
		this.targetProperty = new GenericProperty(targetName);
	}

	public Object transformOutput(final ActionContext actionContext, final GraphObject source) throws FrameworkException {

		if (outputFunction == null) {
			return source.getProperty(sourceProperty);
		}

		// output transformation requested
		actionContext.setConstant("input", source);
		return Scripting.evaluate(actionContext, null, "${" + outputFunction + "}");
	}

	public void transformInput(final ActionContext actionContext, final Map<String, Object> source) throws FrameworkException {

		// move / rename input value
		Object inputValue = source.remove(targetName);
		if (inputValue != null) {

			if (inputFunction != null) {

				// input transformation requested
				actionContext.setConstant("input", inputValue);
				inputValue = Scripting.evaluate(actionContext, null, "${" + inputFunction + "}");
			}

			source.put(sourceName, inputValue);
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
