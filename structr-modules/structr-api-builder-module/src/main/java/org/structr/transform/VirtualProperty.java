package org.structr.transform;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.NodeTrait;

public interface VirtualProperty extends NodeTrait {

	Integer getPosition();
	String getSourceName();
	String getTargetName();
	String getOutputFunction();
	String getInputFunction();
	Transformation getTransformation(String _type) throws FrameworkException;
}
