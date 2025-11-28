/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.docs.Signature;
import org.structr.docs.Usage;

import java.io.IOException;
import java.util.List;

public class UserChangelogFunction extends ChangelogFunction {

	@Override
	public String getName() {
		return "userChangelog";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("user [, resolve=false [, filterKey, filterValue ]... ]");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${userChangelog(user[, resolve=false[, filterKey, filterValue...]])}. Example: ${userChangelog(current, false, 'verb', 'change', 'timeTo', now)}"),
			Usage.javaScript("Usage: ${{Structr.userChangelog(user[, resolve=false[, filterObject]])}}. Example: ${{Structr.userChangelog(Structr.get('me'), false, {verb:\"change\", timeTo: new Date()}))}}")
		);
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
