/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.List;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.javaparser.entity.relation.ModulePackages;
import org.structr.javaparser.entity.relation.PackageChildren;
import org.structr.javaparser.entity.relation.PackageClasses;
import org.structr.javaparser.entity.relation.PackageFolder;
import org.structr.javaparser.entity.relation.PackageSiblings;
import org.structr.web.entity.Folder;
import org.structr.web.property.PathProperty;

/**
 *
 */
public class Package extends LinkedTreeNode<PackageChildren, PackageSiblings, org.structr.javaparser.entity.Package> {
	
	public static final Property<String>                                      path     = new PathProperty("path").indexed().readOnly();
	public static final Property<org.structr.javaparser.entity.Package>       parent   = new StartNode<>("parent", PackageChildren.class);
	public static final Property<List<org.structr.javaparser.entity.Package>> children = new EndNodes<>("children", PackageChildren.class);
	
	public static final Property<Module> module                    = new StartNode<>("module", ModulePackages.class);
	public static final Property<Folder> folder                    = new EndNode<>("folder", PackageFolder.class);
	public static final Property<List<JavaClass>> classes          = new EndNodes<>("classes", PackageClasses.class);
	
	public static final View defaultView = new View(org.structr.javaparser.entity.Package.class, PropertyView.Public, name, path, parent);
	public static final View uiView      = new View(org.structr.javaparser.entity.Package.class, PropertyView.Ui,     name, path, parent, children, classes);

	@Override
	public java.lang.Class<PackageChildren> getChildLinkType() {
		return PackageChildren.class;
	}

	@Override
	public java.lang.Class<PackageSiblings> getSiblingLinkType() {
		return PackageSiblings.class;
	}
}
