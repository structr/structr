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
import org.structr.core.entity.LinkedTreeNodeImpl;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.javaparser.entity.relation.ModulePackages;
import org.structr.javaparser.entity.relation.PackageChildren;
import org.structr.javaparser.entity.relation.PackageClasses;
import org.structr.javaparser.entity.relation.PackageFolder;
import org.structr.javaparser.entity.relation.PackageSibling;
import org.structr.web.entity.Folder;
import org.structr.web.property.PathProperty;

/**
 *
 */
public class Package extends LinkedTreeNodeImpl<org.structr.javaparser.entity.Package> {

	/*static class Impl { static {

		final JsonSchema schema        = SchemaService.getDynamicSchema();
		final JsonObjectType type      = schema.addType("Package");
		final JsonObjectType module    = (JsonObjectType) schema.addType("Module");
		final JsonObjectType folder    = (JsonObjectType) schema.addType("Folder");
		final JsonObjectType javaClass = (JsonObjectType) schema.addType("JavaClass");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Package"));

		type.addStringProperty("path", PropertyView.Public, PropertyView.Ui).setIndexed(true).setReadOnly(true);

		final JsonReferenceType contains = type.relate(type, "CONTAINS", Cardinality.OneToMany, "parent",   "children");

		final JsonReferenceType modulePackages = type.relate(module,    "MODULE_PACKAGE", Cardinality.OneToMany, "module", "packages");
		final JsonReferenceType packageFolder  = type.relate(folder,    "PACKAGE_FOLDER", Cardinality.OneToOne, "folder", "package");
		final JsonReferenceType packageClass   = type.relate(javaClass, "PACKAGE_CLASS",  Cardinality.OneToMany, "classes", "package");

		type.addViewProperty(PropertyView.Ui, contains.getSourcePropertyName());
		type.addViewProperty(PropertyView.Ui, contains.getTargetPropertyName());
		type.addViewProperty(PropertyView.Ui, modulePackages.getSourcePropertyName());
		type.addViewProperty(PropertyView.Ui, packageFolder.getSourcePropertyName());
		type.addViewProperty(PropertyView.Ui, packageClass.getSourcePropertyName());

		type.overrideMethod("getSiblingLinkType",          false, "return PackageCONTAINS_NEXT_SIBLINGPackage.class;");
		type.overrideMethod("getChildLinkType",            false, "return PackageCONTAINSPackage.class;");
	}}*/

	public static final Property<Integer>                                     position = new IntProperty("position").indexed().readOnly();
	public static final Property<String>                                      path     = new PathProperty("path").indexed().readOnly();

	public static final Property<org.structr.javaparser.entity.Package>       parent   = new StartNode<>("parent", PackageChildren.class);
	public static final Property<Iterable<org.structr.javaparser.entity.Package>> children = new EndNodes<>("children", PackageChildren.class);

	public static final Property<Module> module                    = new StartNode<>("module", ModulePackages.class);
	public static final Property<Folder> folder                    = new EndNode<>("folder", PackageFolder.class);
	public static final Property<Iterable<JavaClass>> classes          = new EndNodes<>("classes", PackageClasses.class);

	public static final View defaultView = new View(org.structr.javaparser.entity.Package.class, PropertyView.Public, name, path, parent);
	public static final View uiView      = new View(org.structr.javaparser.entity.Package.class, PropertyView.Ui,     name, path, parent, children, classes);

	@Override
	public java.lang.Class<PackageChildren> getChildLinkType() {
		return PackageChildren.class;
	}

	@Override
	public java.lang.Class<PackageSibling> getSiblingLinkType() {
		return PackageSibling.class;
	}

	@Override
	public PropertyKey<Integer> getPositionProperty() {
		return position;
	}
}
