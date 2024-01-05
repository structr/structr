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
package org.structr.web.entity.mail;

import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.OneToOne;
import org.structr.web.entity.dom.Content;

/**
 *
 *
 */
public class TemplateText extends OneToOne<MailTemplate, Content> {

	@Override
	public Class<MailTemplate> getSourceType() {
		return MailTemplate.class;
	}

	@Override
	public Class<Content> getTargetType() {
		return Content.class;
	}

	@Override
	public String name() {
		return "CONTAINS";
	}

	@Override
	public boolean isInternal() {
		return true;
	}
}
