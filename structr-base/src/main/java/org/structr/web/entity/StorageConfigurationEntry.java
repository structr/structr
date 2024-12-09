package org.structr.web.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.NodeTrait;

public interface StorageConfigurationEntry extends NodeTrait {

	StorageConfiguration getConfiguration();
	void setName(final String name) throws FrameworkException;
	String getValue();
	void setValue(final String value) throws FrameworkException;
}