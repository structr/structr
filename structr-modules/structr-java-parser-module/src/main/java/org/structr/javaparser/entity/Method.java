/*
 * Copyright (C) 2010-2021 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.javaparser.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.javaparser.entity.relation.ClassMethods;
import org.structr.javaparser.entity.relation.MethodsCallMethods;

/**
 *
 */
public class Method extends AbstractNode {

	public static final Property<org.structr.javaparser.entity.ClassOrInterface> classOrInterface   = new StartNode<>("classOrInterface", ClassMethods.class);
	public static final Property<String>    declaration                                             = new StringProperty("declaration").indexed();
	public static final Property<String>    body                                                    = new StringProperty("body"); // .indexed();
	public static final Property<Boolean>   resolved                                                = new BooleanProperty("resolved").indexed();

	public static final Property<Iterable<Method>> callingMethods                                   = new StartNodes<>("callingMethods", MethodsCallMethods.class);
	public static final Property<Iterable<Method>> methodsCalled                                    = new EndNodes<>("methodsCalled", MethodsCallMethods.class);

	public static final View defaultView = new View(Method.class, PropertyView.Public, name, classOrInterface);
	public static final View uiView      = new View(Method.class, PropertyView.Ui,     name, classOrInterface, body, resolved, callingMethods, methodsCalled);
}
