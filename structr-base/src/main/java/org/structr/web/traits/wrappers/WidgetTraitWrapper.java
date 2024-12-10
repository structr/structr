package org.structr.web.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractTraitWrapper;
import org.structr.web.entity.Widget;

public class WidgetTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements Widget {

	public WidgetTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public String getSource() {
		return wrappedObject.getProperty(traits.key(""));
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key("description"));
	}

	@Override
	public boolean isWidget() {
		return wrappedObject.getProperty(traits.key("isWidget"));
	}

	@Override
	public String getTreePath() {
		return wrappedObject.getProperty(traits.key("treePath"));
	}

	@Override
	public String getConfiguration() {
		return wrappedObject.getProperty(traits.key("configuration"));
	}

	@Override
	public boolean isPageTemplate() {
		return wrappedObject.getProperty(traits.key("isPageTemplate"));
	}

	@Override
	public String[] getSelectors() {
		return wrappedObject.getProperty(traits.key("selectors"));
	}
}
