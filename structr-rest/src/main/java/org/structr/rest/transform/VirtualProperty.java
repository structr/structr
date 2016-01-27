package org.structr.rest.transform;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;

/**
 *
 * @author Christian Morgner
 */
public class VirtualProperty extends AbstractNode {

	public static final Property<VirtualType> virtualType = new StartNode<>("virtualType", VirtualTypeProperty.class);
	public static final Property<Integer> position        = new IntProperty("position").indexed();
	public static final Property<String> sourceName       = new StringProperty("sourceName");
	public static final Property<String> targetName       = new StringProperty("targetName");
	public static final Property<String> inputFunction    = new StringProperty("inputFunction");
	public static final Property<String> outputFunction   = new StringProperty("outputFunction");

	public static final View defaultView = new View(VirtualProperty.class, PropertyView.Public,
		virtualType, sourceName, targetName, inputFunction, outputFunction, position
	);

	public static final View uiView = new View(VirtualProperty.class, PropertyView.Ui,
		virtualType, sourceName, targetName, inputFunction, outputFunction, position
	);

	public String getSourceName() {
		return getProperty(sourceName);
	}

	public String getTargetName() {
		return getProperty(targetName);
	}

	public String getOutputFunction() {
		return getProperty(outputFunction);
	}

	public String getInputFunction() {
		return getProperty(inputFunction);
	}

	public Transformation getTransformation(final Class _type) throws FrameworkException {

		final String _sourceName     = getProperty(sourceName);
		final String _inputFunction  = getProperty(inputFunction);
		final String _outputFunction = getProperty(outputFunction);
		String _targetName           = getProperty(targetName);

		if (_sourceName == null) {
			throw new FrameworkException(500, "VirtualProperty with ID " + getUuid() + " needs source name");
		}

		// don't rename
		if (_targetName == null) {
			_targetName = _sourceName;
		}

		return new Transformation(_type, _sourceName, _targetName, _inputFunction, _outputFunction);
	}
}
