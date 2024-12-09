package org.structr.transform;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractTraitWrapper;

public class VirtualPropertyTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements VirtualProperty {

	public VirtualPropertyTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public Integer getPosition() {
		return wrappedObject.getProperty(traits.key("position"));
	}

	@Override
	public String getSourceName() {
		return wrappedObject.getProperty(traits.key("sourceName"));
	}

	@Override
	public String getTargetName() {
		return wrappedObject.getProperty(traits.key("targetName"));
	}

	@Override
	public String getOutputFunction() {
		return wrappedObject.getProperty(traits.key("outputFunction"));
	}

	@Override
	public String getInputFunction() {
		return wrappedObject.getProperty(traits.key("inputFunction"));
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
