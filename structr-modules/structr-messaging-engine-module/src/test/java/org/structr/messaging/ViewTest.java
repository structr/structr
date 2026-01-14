/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.messaging;

import org.structr.core.traits.StructrTraits;
import org.structr.messaging.traits.definitions.MQTTClientTraitDefinition;
import org.structr.web.common.TestHelper;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ViewTest extends MessagingTestBase {

	@Test
	public void testViews() {

		final Map<String, List<String>> additionalRequiredAttributes = new HashMap<>();

		additionalRequiredAttributes.put(StructrTraits.MQTT_CLIENT, List.of(MQTTClientTraitDefinition.MAIN_BROKER_URL_PROPERTY));

		TestHelper.testViews(app, ViewTest.class.getResourceAsStream("/views.properties"), additionalRequiredAttributes);
	}
}