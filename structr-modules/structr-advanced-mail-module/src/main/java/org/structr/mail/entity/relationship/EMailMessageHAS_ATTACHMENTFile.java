/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.mail.entity.relationship;


import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.common.PermissionPropagation;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.mail.entity.EMailMessage;
import org.structr.web.entity.File;

public class EMailMessageHAS_ATTACHMENTFile extends OneToMany<EMailMessage, File> implements PermissionPropagation {

	@Override
	public Class<EMailMessage> getSourceType() {
		return EMailMessage.class;
	}

	@Override
	public Class<File> getTargetType() {
		return File.class;
	}

	@Override
	public String name() {
		return "HAS_ATTACHMENT";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}

	@Override
	public PropagationDirection getPropagationDirection() {
		return PropagationDirection.Out;
	}

	@Override
	public PropagationMode getReadPropagation() {
		return PropagationMode.Add;
	}

	@Override
	public PropagationMode getWritePropagation() {
		return PropagationMode.Remove;
	}

	@Override
	public PropagationMode getDeletePropagation() {
		return PropagationMode.Remove;
	}

	@Override
	public PropagationMode getAccessControlPropagation() {
		return PropagationMode.Remove;
	}

	@Override
	public String getDeltaProperties() {
		return "";
	}

}
