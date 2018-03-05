/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.messaging.engine.entities;

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.messaging.engine.relation.MessageClientHASMessageSubscriber;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageSubscriber extends AbstractNode {
    public static final Property<List<MessageClient>> clients         = new StartNodes<>("clients", MessageClientHASMessageSubscriber.class);
    public static final Property<String> topic                        = new StringProperty("topic");
    public static final Property<String> callback                     = new StringProperty("callback");

    private static final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class.getName());

    public static final View publicView = new View(MessageSubscriber.class, PropertyView.Public,
            clients, topic, callback
    );

    public static final View uiView = new View(MessageSubscriber.class, PropertyView.Ui,
            clients, topic, callback
    );

    static {

        SchemaService.registerBuiltinTypeOverride("MessageSubscriber", MessageSubscriber.class.getName());

    }

    protected void subscribeOnAllClients() {
		if(!StringUtils.isEmpty(getProperty(MessageSubscriber.topic)) && (getProperty(MessageSubscriber.clients) != null)) {
			Map<String,Object> params = new HashMap<>();
			params.put("topic", getProperty(MessageSubscriber.topic));

			List<MessageClient> clients = getProperty(MessageSubscriber.clients);
			clients.forEach(client -> {
				try {
					client.invokeMethod("subscribeTopic", params, false);
				} catch (FrameworkException e) {
					logger.error("Could not invoke subscribeTopic on MessageClient: " + e.getMessage());
				}
			});
		}
	}

    @Override
    public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		subscribeOnAllClients();
        return super.onCreation(securityContext, errorBuffer);
    }

    @Override
    public boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

        if(modificationQueue.isPropertyModified(this, MessageSubscriber.topic) || modificationQueue.isPropertyModified(this, MessageSubscriber.clients)) {
			subscribeOnAllClients();
        }

        return super.onModification(securityContext, errorBuffer, modificationQueue);
    }

    @Export
    public RestMethodResult onMessage(final String topic, final String message) throws FrameworkException {

        if (!StringUtils.isEmpty(getProperty(callback))) {

            String script = "${" + getProperty(callback) + "}";

            Map<String, Object> params = new HashMap<>();
            params.put("topic", topic);
            params.put("message", message);

            ActionContext ac = new ActionContext(securityContext, params);
            Scripting.replaceVariables(ac, this, script);
        }

        return new RestMethodResult(200);
    }

}
