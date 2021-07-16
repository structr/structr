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
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.javaparser.entity.relation.ClassMethods;
import org.structr.javaparser.entity.relation.PackageClasses;

/**
 *
 */
public class ClassOrInterface extends AbstractNode {

	public static final Property<org.structr.javaparser.entity.Package> packageProp     = new StartNode<>("package", PackageClasses.class);
	public static final Property<Iterable<Method>>                          methods     = new EndNodes<>("methods",  ClassMethods.class);

	public static final View defaultView = new View(ClassOrInterface.class, PropertyView.Public, name, packageProp, methods);
	public static final View uiView      = new View(ClassOrInterface.class, PropertyView.Ui,     name, packageProp, methods);
}
