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
package org.structr.mqtt.entity.relation;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.mqtt.entity.MQTTClient;
import org.structr.mqtt.entity.MQTTSubscriber;

public class MQTTClientHAS_SUBSCRIBERMQTTSubscriber extends OneToMany<MQTTClient, MQTTSubscriber>{

	@Override
	public Class<MQTTClient> getSourceType() {
		return MQTTClient.class;
	}

	@Override
	public Class<MQTTSubscriber> getTargetType() {
		return MQTTSubscriber.class;
	}

	@Override
	public String name() {
		return "HAS_SUBSCRIBER";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

}
