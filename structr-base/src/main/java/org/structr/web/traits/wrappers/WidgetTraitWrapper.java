/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.traits.wrappers;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.web.entity.Widget;
import org.structr.web.traits.definitions.WidgetTraitDefinition;

public class WidgetTraitWrapper extends AbstractNodeTraitWrapper implements Widget {

	public WidgetTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	@Override
	public String getSource() {
		return wrappedObject.getProperty(traits.key(WidgetTraitDefinition.SOURCE_PROPERTY));
	}

	@Override
	public String getDescription() {
		return wrappedObject.getProperty(traits.key(WidgetTraitDefinition.DESCRIPTION_PROPERTY));
	}

	@Override
	public boolean isWidget() {
		return wrappedObject.getProperty(traits.key(WidgetTraitDefinition.IS_WIDGET_PROPERTY));
	}

	@Override
	public String getTreePath() {
		return wrappedObject.getProperty(traits.key(WidgetTraitDefinition.TREE_PATH_PROPERTY));
	}

	@Override
	public String getConfiguration() {
		return wrappedObject.getProperty(traits.key(WidgetTraitDefinition.CONFIGURATION_PROPERTY));
	}

	@Override
	public boolean isPageTemplate() {
		return wrappedObject.getProperty(traits.key(WidgetTraitDefinition.IS_PAGE_TEMPLATE_PROPERTY));
	}

	@Override
	public String[] getSelectors() {
		return wrappedObject.getProperty(traits.key(WidgetTraitDefinition.SELECTORS_PROPERTY));
	}
}
