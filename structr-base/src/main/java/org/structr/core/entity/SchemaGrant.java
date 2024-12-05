package org.structr.core.entity;

import org.structr.core.traits.NodeTrait;

public interface SchemaGrant extends NodeTrait {

	String getPrincipalId();
	String getPrincipalName();
}
