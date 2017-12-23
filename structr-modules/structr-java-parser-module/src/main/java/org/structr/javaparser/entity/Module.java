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
import org.structr.javaparser.entity.relation.ModuleChildren;
import org.structr.javaparser.entity.relation.ModuleFolder;
import org.structr.javaparser.entity.relation.ModulePackages;
import org.structr.javaparser.entity.relation.ModuleSiblings;
import org.structr.web.entity.Folder;
import org.structr.web.property.PathProperty;

/**
 *
 */
public class Module extends LinkedTreeNode<ModuleChildren, ModuleSiblings, Module> {
	
	public static final Property<String> path                      = new PathProperty("path").indexed().readOnly();
	public static final Property<Module> parent                    = new StartNode<>("parent", ModuleChildren.class);
	public static final Property<List<Module>> children            = new EndNodes<>("children", ModuleChildren.class);
	
	public static final Property<Folder> folder                    = new EndNode<>("folder", ModuleFolder.class);
	public static final Property<List<org.structr.javaparser.entity.Package>> packages           = new EndNodes<>("packages", ModulePackages.class);
	
	public static final View defaultView = new View(Module.class, PropertyView.Public, name, path, parent);
	public static final View uiView      = new View(Module.class, PropertyView.Ui,     name, path, parent, children, packages);

	@Override
	public java.lang.Class<ModuleChildren> getChildLinkType() {
		return ModuleChildren.class;
	}

	@Override
	public java.lang.Class<ModuleSiblings> getSiblingLinkType() {
		return ModuleSiblings.class;
	}
}
