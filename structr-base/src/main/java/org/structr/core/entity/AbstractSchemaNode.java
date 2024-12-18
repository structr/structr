package org.structr.core.entity;

import org.structr.common.error.FrameworkException;
import org.structr.core.traits.NodeTrait;

import java.util.List;

public interface AbstractSchemaNode extends NodeTrait {

	Iterable<SchemaProperty> getSchemaProperties();
	Iterable<SchemaView> getSchemaViews();
	Iterable<SchemaMethod> getSchemaMethods();
	Iterable<SchemaGrant> getSchemaGrants();

	Iterable<SchemaMethod> getSchemaMethodsIncludingInheritance();
	SchemaMethod getSchemaMethod(final String name);
	List<SchemaMethod> getSchemaMethodsByName(final String name);
	SchemaProperty getSchemaProperty(final String name);
	SchemaView getSchemaView(final String name);

	SchemaNode getExtendsClass();

	String getExtendsClassInternal();
	String getImplementsInterfaces();

	String getSummary();
	String getIcon();
	String getDescription();
	String getCategory();
	String getClassName();
	String getDefaultSortKey();
	String getDefaultSortOrder();

	boolean isInterface();
	boolean isAbstract();
	boolean isBuiltinType();
	boolean changelogDisabled();
	boolean defaultVisibleToPublic();
	boolean defaultVisibleToAuth();
	boolean includeInOpenAPI();

	String[] getTags();

	void setExtendsClass(final SchemaNode schemaNode) throws FrameworkException;
}
