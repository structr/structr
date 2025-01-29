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
package org.structr.core.function;

import org.structr.api.config.Settings;
import org.structr.core.GraphObject;

import java.io.IOException;

public class UserChangelogFunction extends ChangelogFunction {

	public static final String ERROR_MESSAGE_USER_CHANGELOG    = "Usage: ${user_changelog(user[, resolve=false[, filterKey, filterValue...]])}. Example: ${user_changelog(current, false, 'verb', 'change', 'timeTo', now)}";
	public static final String ERROR_MESSAGE_USER_CHANGELOG_JS = "Usage: ${{Structr.userChangelog(user[, resolve=false[, filterObject]])}}. Example: ${{Structr.userChangelog(Structr.get('me'), false, {verb:\"change\", timeTo: new Date()}))}}";

	@Override
	public String getName() {
		return "user_changelog";
	}

	@Override
	public String getSignature() {
		return "user [, resolve=false [, filterKey, filterValue ]... ]";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_USER_CHANGELOG_JS : ERROR_MESSAGE_USER_CHANGELOG);
	}

	@Override
	protected String getChangelogForGraphObject (final GraphObject obj) throws IOException {

		return getChangelogForUUID(obj.getUuid(), "u");

	}

	@Override
	protected String getChangelogForString (final String inputString) throws IOException {

		if (Settings.isValidUuid(inputString)) {

			return getChangelogForUUID(inputString, "u");

		} else {

			throw new IllegalArgumentException("Given string is not a UUID: " + inputString);
		}
	}

	@Override
	protected boolean isUserCentric () {
		return true;
	}
}
