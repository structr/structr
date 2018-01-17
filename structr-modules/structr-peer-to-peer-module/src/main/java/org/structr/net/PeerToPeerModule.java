/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.net;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.api.service.LicenseManager;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.module.StructrModule;
import org.structr.schema.action.Actions;


public class PeerToPeerModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {
	}

	@Override
	public String getName() {
		return "peer-to-peer";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final StringBuilder buf) {

		if (isShared(schemaNode)) {

			buf.append("import org.structr.net.SharedNodeInterface;\n");
		}
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {

		final Set<String> interfaces = new LinkedHashSet<>();

		if (isShared(schemaNode)) {
			interfaces.add("SharedNodeInterface");
		}

		return interfaces;
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final StringBuilder buf, final Actions.Type type) {

		if (isShared(schemaNode)) {

			switch (type) {

				case Create:
					buf.append("\n");
					buf.append("\t\tfinal org.structr.net.PeerToPeerService service = getService();\n");
					buf.append("\t\tif (service != null) {\n");
					buf.append("\n");
					buf.append("\t\t\tfinal org.structr.net.data.time.PseudoTime time = service.getTime();\n");
					buf.append("\n");
					buf.append("\t\t\tsuper.setProperty(lastModifiedPseudoTime, time.toString());\n");
					buf.append("\t\t\tsuper.setProperty(createdPseudoTime,      time.toString());\n");
					buf.append("\t\t}\n");
					buf.append("\n");
					break;

				case Save:
					buf.append("\n");
					buf.append("\t\tfinal org.structr.net.PeerToPeerService service = getService();\n");
					buf.append("\t\tif (service != null) {\n");
					buf.append("\n");
					buf.append("\t\t\tservice.update(this);\n");
					buf.append("\t\t}\n");
					buf.append("\n");
					break;

				default:
					break;
			}
		}
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final StringBuilder buf) {

		if (isShared(schemaNode)) {

			buf.append("\tprivate boolean fullyCreated = false;\n");
			buf.append("\n");
			buf.append("\t@Override\n");
			buf.append("\tpublic void onNodeInstantiation(final boolean isCreation) {\n");
			buf.append("\t\tfullyCreated = !isCreation;\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\tpublic Map<String, Object> getData() {\n");
			buf.append("\n");
			buf.append("\t\tfinal Set<PropertyKey> keys    = new LinkedHashSet<>(StructrApp.getConfiguration().getPropertySet(entityType, \"shared\"));\n");
			buf.append("\t\tfinal Map<String, Object> data = new HashMap<>();\n");
			buf.append("\n");
			buf.append("\t\tkeys.removeAll(nativeKeys);\n");
			buf.append("\n");
			buf.append("\t\tfor (final PropertyKey key : keys) {\n");
			buf.append("\n");
			buf.append("\t\t\tfinal PropertyConverter converter = key.inputConverter(securityContext);\n");
			buf.append("\t\t\tObject value = convert(getProperty(key));\n");
			buf.append("\n");
			buf.append("\t\t\tif (converter != null && !(value instanceof String)) {\n");
			buf.append("\n");
			buf.append("\t\t\t\ttry {\n");
			buf.append("\n");
			buf.append("\t\t\t\t\tvalue = converter.revert(value);\n");
			buf.append("\n");
			buf.append("\t\t\t\t} catch (FrameworkException fex) {\n");
			buf.append("\t\t\t\t\tfex.printStackTrace();\n");
			buf.append("\t\t\t\t}\n");
			buf.append("\t\t\t}\n");
			buf.append("\n");
			buf.append("\t\t\tdata.put(key.jsonName(), value);\n");
			buf.append("\t\t}\n");
			buf.append("\n");
			buf.append("\t\treturn data;\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\t@Override\n");
			buf.append("\tpublic void afterCreation(SecurityContext securityContext) {\n");
			buf.append("\n");
			buf.append("\t\tfullyCreated = true;\n");
			buf.append("\n");
			buf.append("\t\tfinal org.structr.net.PeerToPeerService service = getService();\n");
			buf.append("\t\tif (service != null) {\n");
			buf.append("\n");
			buf.append("\t\t\tservice.create(this);\n");
			buf.append("\t\t}\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\t@Override\n");
			buf.append("\tpublic void afterDeletion(final SecurityContext securityContext, final PropertyMap properties) {\n");
			buf.append("\n");
			buf.append("\t\tfinal org.structr.net.PeerToPeerService service = getService();\n");
			buf.append("\t\tif (service != null) {\n");
			buf.append("\n");
			buf.append("\t\t\tservice.delete(properties.get(GraphObject.id));\n");
			buf.append("\t\t}\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\t@Override\n");
			buf.append("\tpublic <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {\n");
			buf.append("\n");
			buf.append("\t\tif (fullyCreated) {\n");
			buf.append("\n");
			buf.append("\t\t\tfinal org.structr.net.PeerToPeerService service = getService();\n");
			buf.append("\t\t\tif (service != null) {\n");
			buf.append("\n");
			buf.append("\t\t\t\tservice.setProperty(getProperty(GraphObject.id), key.jsonName(), convert(value));\n");
			buf.append("\t\t\t}\n");
			buf.append("\t\t}\n");
			buf.append("\n");
			buf.append("\t\treturn super.setProperty(key, value);\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\tpublic org.structr.net.data.time.PseudoTime getCreationPseudoTime() {\n");
			buf.append("\t\treturn org.structr.net.data.time.PseudoTime.fromString(getProperty(createdPseudoTime));\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\tpublic org.structr.net.data.time.PseudoTime getLastModificationPseudoTime() {\n");
			buf.append("\t\treturn org.structr.net.data.time.PseudoTime.fromString(getProperty(lastModifiedPseudoTime));\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\tpublic void setProperty(final org.structr.core.app.App app, final PropertyKey key, final Object rawValue) throws FrameworkException {\n");
			buf.append("\n");
			buf.append("\t\tfinal PropertyConverter inputConverter = key.inputConverter(securityContext);\n");
			buf.append("\t\tObject value                           = revert(app, rawValue);\n");
			buf.append("\n");
			buf.append("\t\tif (inputConverter != null) {\n");
			buf.append("\t\t\tvalue = inputConverter.convert(value);\n");
			buf.append("\t\t}\n");
			buf.append("\n");
			buf.append("\t\tsuper.setProperty(key, value);\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\tpublic String getUserId() {\n");
			buf.append("\n");
			buf.append("\t\tfinal Principal owner = getOwnerNode();\n");
			buf.append("\t\tif (owner != null) {\n");
			buf.append("\n");
			buf.append("\t\t\treturn owner.getName();\n");
			buf.append("\t\t}\n");
			buf.append("\n");
			buf.append("\t\treturn Principal.SUPERUSER_ID;\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\t// ----- private methods -----\n");
			buf.append("\tprivate org.structr.net.PeerToPeerService getService() {\n");
			buf.append("\t\treturn Services.getInstance().getService(org.structr.net.PeerToPeerService.class);\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\tprivate Object convert(final Object value) {\n");
			buf.append("\n");
			buf.append("\t\tObject result = value;\n");
			buf.append("\n");
			buf.append("\t\tif (value instanceof GraphObject) {\n");
			buf.append("\n");
			buf.append("\t\t\tresult = \"!\" + ((GraphObject)value).getUuid();\n");
			buf.append("\n");
			buf.append("\t\t} else if (value instanceof Collection) {\n");
			buf.append("\n");
			buf.append("\t\t\tfinal List<Object> list = new LinkedList<>();\n");
			buf.append("\t\t\tfor (final Object item : ((Collection)value)) {\n");
			buf.append("\n");
			buf.append("\t\t\t\t// recurse\n");
			buf.append("\t\t\t\tlist.add(convert(item));\n");
			buf.append("\t\t\t}\n");
			buf.append("\n");
			buf.append("\t\t\tresult = \"#\" + StringUtils.join(list, \",\");\n");
			buf.append("\t\t}\n");
			buf.append("\n");
			buf.append("\t\treturn result;\n");
			buf.append("\t}\n");
			buf.append("\n");
			buf.append("\tprivate Object revert(final org.structr.core.app.App app, final Object value) throws FrameworkException {\n");
			buf.append("\n");
			buf.append("\t\tObject result = value;\n");
			buf.append("\n");
			buf.append("\t\tif (value instanceof String) {\n");
			buf.append("\n");
			buf.append("\t\t\tfinal String str = (String)value;\n");
			buf.append("\n");
			buf.append("\t\t\tif (str.startsWith(\"#\")) {\n");
			buf.append("\n");
			buf.append("\t\t\t\tif (str.length() > 1) {\n");
			buf.append("\n");
			buf.append("\t\t\t\t\tfinal String[] parts    = str.substring(1).split(\"[,]+\");\n");
			buf.append("\t\t\t\t\tfinal List<Object> list = new LinkedList<>();\n");
			buf.append("\n");
			buf.append("\t\t\t\t\tfor (final String part : parts) {\n");
			buf.append("\t\t\t\t\t\tlist.add(revert(app, part));\n");
			buf.append("\t\t\t\t\t}\n");
			buf.append("\n");
			buf.append("\t\t\t\t\tresult = list;\n");
			buf.append("\n");
			buf.append("\t\t\t\t} else {\n");
			buf.append("\n");
			buf.append("\t\t\t\t\tresult = Collections.emptyList();\n");
			buf.append("\t\t\t\t}\n");
			buf.append("\n");
			buf.append("\t\t\t} else if (str.startsWith(\"!\") && str.length() == 33) {\n");
			buf.append("\n");
			buf.append("\t\t\t\tresult = app.get(str.substring(1));\n");
			buf.append("\t\t\t}\n");
			buf.append("\t\t}\n");
			buf.append("\n");
			buf.append("\t\treturn result;\n");
			buf.append("\t}\n");

		}
	}

	// ----- private methods -----
	private boolean isShared(final AbstractSchemaNode schemaNode) {
		return Boolean.TRUE.equals(schemaNode.getProperty(SchemaNode.shared));
	}
}
