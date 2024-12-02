/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.web.entity.html.relationship.LinkSourceLINKLinkable;

/**
 *
 */
public interface Linkable extends NodeInterface {

	Property<Iterable<LinkSource>> linkingElementsProperty = new StartNodes<>("linkingElements", LinkSourceLINKLinkable.class).partOfBuiltInSchema().partOfBuiltInSchema();
	Property<Iterable<String>> linkinkElementsIdsProperty  = new CollectionIdProperty<>("linkingElementsIds", linkingElementsProperty).partOfBuiltInSchema().partOfBuiltInSchema();
	Property<Boolean> enableBasicAuthProperty              = new BooleanProperty("enableBasicAuth").defaultValue(false).indexed().partOfBuiltInSchema();
	Property<String> basicAuthRealmProperty                = new StringProperty("basicAuthRealm").partOfBuiltInSchema();

	View uiView = new View(Linkable.class, PropertyView.Ui,
		linkingElementsProperty, linkinkElementsIdsProperty, enableBasicAuthProperty, basicAuthRealmProperty
	);

	default boolean getEnableBasicAuth() {
		return getProperty(enableBasicAuthProperty);
	}

	default String getBasicAuthRealm() {
		return getProperty(basicAuthRealmProperty);
	}

	String getPath();
}
