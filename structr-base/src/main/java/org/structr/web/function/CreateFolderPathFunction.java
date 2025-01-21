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
package org.structr.web.function;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyMap;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.action.ActionContext;
import org.structr.web.common.FileHelper;

import java.util.Map;

public class CreateFolderPathFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_CREATE_FOLDER_PATH    = "Usage: ${create_folder_path(path)}. Example: ${create_folder_path(\"/img/icons/large\")}";
	public static final String ERROR_MESSAGE_CREATE_FOLDER_PATH_JS = "Usage: ${{ Structr.createFolderPath({ path: value})}}. Example: ${{ Structr.createFolderPath({ path: \"/img/icons/large\"})}}";

	@Override
	public String getName() {
		return "create_folder_path";
	}

	@Override
	public String getSignature() {
		return "type [, parameterMap ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length > 0) {

			final SecurityContext securityContext = ctx.getSecurityContext();
			final ConfigurationProvider config = StructrApp.getConfiguration();
			PropertyMap propertyMap;
			String path = null;

			// extension for native JavaScript objects
			if (sources.length == 1 && sources[0] instanceof Map) {

				path = ((Map) sources[0]).get("path").toString();

			} else if (sources.length == 1 && sources[0] instanceof GraphObjectMap) {

				path = ((GraphObjectMap) sources[0]).toMap().get("path").toString();

			} else if (sources.length == 1 && sources[0] instanceof String) {

				path = (String) sources[0];
			}

			return FileHelper.createFolderPath(securityContext, path);

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CREATE_FOLDER_PATH_JS : ERROR_MESSAGE_CREATE_FOLDER_PATH);
	}

	@Override
	public String shortDescription() {
		return "Creates a new folder in the virtual file system including all parent folders if they don't exist already.";
	}

}
