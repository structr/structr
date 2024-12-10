package org.structr.web.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractTraitWrapper;
import org.structr.web.entity.Linkable;

public class LinkableTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements Linkable {

	public LinkableTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public boolean getEnableBasicAuth() {
		return wrappedObject.getProperty(traits.key("enableBasicAuth"));
	}

	@Override
	public String getBasicAuthRealm() {
		return wrappedObject.getProperty(traits.key("basicAuthRealm"));
	}

	@Override
	public String getPath() {
		return wrappedObject.getProperty(traits.key("path"));
	}
}
