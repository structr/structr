/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.entity;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

/**
 *
 *
 */
public abstract class AbstractSchemaLocalization extends AbstractNode {

	public static final Property<String>                  locale = new StringProperty("locale");

	public static final View defaultView = new View(AbstractSchemaLocalization.class, PropertyView.Public,
		locale, name
	);

	public static final View uiView = new View(AbstractSchemaLocalization.class, PropertyView.Ui,
		locale, name
	);
	
	@Override
	public void onNodeCreation() {
		try {

			setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
			setProperty(AbstractNode.visibleToPublicUsers, true);

		} catch (FrameworkException ex) {

			Logger.getLogger(AbstractSchemaLocalization.class.getName()).log(Level.WARNING, "Unable to set the visibility flags for Localization", ex);

		}
	}
}
