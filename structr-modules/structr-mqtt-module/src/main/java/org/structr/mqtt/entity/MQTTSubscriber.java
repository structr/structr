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
package org.structr.mqtt.entity;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.apache.cxf.common.util.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.Scripting;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

public interface MQTTSubscriber extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("MQTTSubscriber");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/MQTTSubscriber"));

		type.addStringProperty("topic",  PropertyView.Public);
		type.addStringProperty("source", PropertyView.Public);

		type.addPropertyGetter("topic",  String.class);
		type.addPropertyGetter("source", String.class);
		type.addPropertyGetter("client", MQTTClient.class);

		type.overrideMethod("onCreation", true, MQTTSubscriber.class.getName() + ".onCreation(this, arg0, arg1);");

		type.addMethod("onMessage")
			.setReturnType(RestMethodResult.class.getName())
			.addParameter("topic",   String.class.getName())
			.addParameter("message", String.class.getName())
			.setSource("return " + MQTTSubscriber.class.getName() + ".onMessage(this, topic, message);")
			.addException(FrameworkException.class.getName())
			.setDoExport(true);
	}}

	String getTopic();
	String getSource();
	MQTTClient getClient();

	/*

	private static final Logger logger = LoggerFactory.getLogger(MQTTSubscriber.class.getName());

	public static final Property<MQTTClient>		client			= new StartNode<>("client", MQTTClientHAS_SUBSCRIBERMQTTSubscriber.class);
	public static final Property<String>			topic			= new StringProperty("topic");
	public static final Property<String>			source			= new StringProperty("source");

	public static final View defaultView = new View(MQTTClient.class, PropertyView.Public, id, type, client, topic, source);

	public static final View uiView = new View(MQTTClient.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
        client, topic, source
	);

	static {

		SchemaService.registerBuiltinTypeOverride("MQTTSubscriber", MQTTSubscriber.class.getName());
	}
	*/

	static void onCreation(final MQTTSubscriber thisSubscriber, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		final MQTTClient client = thisSubscriber.getClient();

		if(!StringUtils.isEmpty(thisSubscriber.getTopic()) && (client != null) && client.getIsConnected()) {

			Map<String,Object> params = new HashMap<>();

			params.put("topic", thisSubscriber.getTopic());

			client.invokeMethod("subscribeTopic", params, false);
		}
	}

	static void onModification(final MQTTSubscriber thisSubscriber, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		final PropertyKey<String> topic = StructrApp.key(MQTTSubscriber.class, "topic");
		final MQTTClient client         = thisSubscriber.getClient();

		if(!StringUtils.isEmpty(thisSubscriber.getTopic()) && (client != null) && client.getIsConnected()) {

			if(modificationQueue.isPropertyModified(thisSubscriber, topic)){

				Map<String,Object> params = new HashMap<>();

				params.put("topic", thisSubscriber.getTopic());

				client.invokeMethod("subscribeTopic", params, false);
			}
		}
	}

	static RestMethodResult onMessage(final MQTTSubscriber thisSubscriber, final String topic, final String message) throws FrameworkException {

		final String source = thisSubscriber.getSource();

		if (!StringUtils.isEmpty(source)) {

			final String script              = "${" + source + "}";
			final Map<String, Object> params = new HashMap<>();

			params.put("topic", topic);
			params.put("message", message);

			final ActionContext ac = new ActionContext(thisSubscriber.getSecurityContext(), params);

			Scripting.replaceVariables(ac, thisSubscriber, script);
		}

		return new RestMethodResult(200);
	}
}
