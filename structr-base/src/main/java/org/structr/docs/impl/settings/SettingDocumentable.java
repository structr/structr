/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.docs.impl.settings;

import org.structr.api.config.Setting;
import org.structr.docs.*;

import java.util.List;

public class SettingDocumentable implements Documentable {

	final Setting setting;

	public SettingDocumentable(final Setting setting) {
		this.setting = setting;
	}

	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.Setting;
	}

	@Override
	public String getName() {
		return setting.getKey();
	}

	@Override
	public String getShortDescription() {
		return setting.getComment();
	}

	@Override
	public String getLongDescription() {
		return null;
	}

	@Override
	public List<Parameter> getParameters() {
		return null;
	}

	@Override
	public List<Example> getExamples() {
		return null;
	}

	@Override
	public List<String> getNotes() {
		return null;
	}

	@Override
	public List<Signature> getSignatures() {
		return null;
	}

	@Override
	public List<Language> getLanguages() {
		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return null;
	}
}
