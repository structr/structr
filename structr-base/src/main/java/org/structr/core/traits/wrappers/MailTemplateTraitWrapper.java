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
package org.structr.core.traits.wrappers;

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.MailTemplate;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;

public class MailTemplateTraitWrapper extends AbstractNodeTraitWrapper implements MailTemplate {

	public MailTemplateTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getText() {
		return wrappedObject.getProperty(traits.key(MailTemplateTraitDefinition.TEXT_PROPERTY));
	}

	public String getLocale() {
		return wrappedObject.getProperty(traits.key(MailTemplateTraitDefinition.LOCALE_PROPERTY));
	}

	public void setLocale(final String locale) throws FrameworkException {
		wrappedObject.setProperty(traits.key(MailTemplateTraitDefinition.LOCALE_PROPERTY), locale);
	}
}
