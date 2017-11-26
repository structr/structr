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
package org.structr.web.entity;

import java.net.URI;
import java.util.List;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;


public interface Folder extends AbstractFile, CMISInfo, CMISFolderInfo {

	static class Impl { static {

		final JsonSchema schema = SchemaService.getDynamicSchema();
		final JsonType type     = schema.addType("Folder");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Folder"));
		type.setExtends(URI.create("#/definitions/AbstractFile"));

		type.addBooleanProperty("isFolder").addTransformer(ConstantBooleanTrue.class.getName());
		type.addIntegerProperty("position", PropertyView.Public).setIndexed(true);

		type.overrideMethod("onCreation",     true, "org.structr.web.entity.Folder.setHasParent(this);");
		type.overrideMethod("onModification", true, "org.structr.web.entity.Folder.setHasParent(this);");

		// ----- CMIS support -----
		type.overrideMethod("getCMISInfo",         false, "return this;");
		type.overrideMethod("getBaseTypeId",       false, "return BaseTypeId.CMIS_FOLDER;");
		type.overrideMethod("getFolderInfo",       false, "return this;");
		type.overrideMethod("getDocumentInfo",     false, "return null;");
		type.overrideMethod("getItemInfo",         false, "return null;");
		type.overrideMethod("getRelationshipInfo", false, "return null;");
		type.overrideMethod("getPolicyInfo",       false, "return null;");
		type.overrideMethod("getSecondaryInfo",    false, "return null;");
		type.overrideMethod("getParentId",         false, "return getProperty(parentIdProperty);");
		type.overrideMethod("getPath",             false, "return getProperty(pathProperty);");
		type.overrideMethod("getAllowableActions", false, "return StructrFolderActions.getInstance();");
		type.overrideMethod("getChangeToken",      false, "return null;");

	}}

	List<Folder> getFolders();
	List<File> getFiles();

	static void setHasParent(final Folder folder) throws FrameworkException {

		synchronized (folder) {

			// save current security context
			final SecurityContext previousSecurityContext = folder.getSecurityContext();
			final PropertyKey<Boolean> hasParentKey       = StructrApp.getConfiguration().getPropertyKeyForJSONName(Folder.class, "hasParent");
			final PropertyKey<String> parentIdKey         = StructrApp.getConfiguration().getPropertyKeyForJSONName(Folder.class, "parentId");

			// replace with SU context
			folder.setSecurityContext(SecurityContext.getSuperUserInstance());

			// set property as super user
			folder.setProperty(hasParentKey, folder.getProperty(parentIdKey) != null);

			// restore previous security context
			folder.setSecurityContext(previousSecurityContext);
		}
	}

	/*TODO:

		public static final Property<List<Folder>>   folders                 = new EndNodes<>("folders", Folders.class, new PropertySetNotion(id, name));
		public static final Property<List<FileBase>> files                   = new EndNodes<>("files", Files.class, new PropertySetNotion(id, name));
		public static final Property<List<Image>>    images                  = new EndNodes<>("images", Images.class, new PropertySetNotion(id, name));
		public static final Property<User>           homeFolderOfUser        = new StartNode<>("homeFolderOfUser", UserHomeDir.class);
	*/
}
