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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.messaging.engine.relation.MessageClientHASMessageSubscriber;
import org.structr.rest.RestMethodResult;
import org.structr.schema.SchemaService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageClient extends AbstractNode {
    public static final Property<List<MessageSubscriber>>	subscribers		= new EndNodes<>("subscribers", MessageClientHASMessageSubscriber.class);

    private static final Logger logger = LoggerFactory.getLogger(MessageClient.class.getName());

    public static final View publicView = new View(MessageSubscriber.class, PropertyView.Public,
            subscribers
    );

    public static final View uiView = new View(MessageSubscriber.class, PropertyView.Ui,
            subscribers
    );

    static {

        SchemaService.registerBuiltinTypeOverride("MessageClient", MessageClient.class.getName());
    }

    @Export
    public RestMethodResult sendMessage(final String topic, final String message) throws FrameworkException {

        List<MessageSubscriber> subscribers = getProperty(MessageClient.subscribers);
        if(subscribers != null) {
          subscribers.forEach(sub -> {
              if(sub.getProperty(MessageSubscriber.topic).equals(topic)) {
                  Map<String, Object> params = new HashMap<>();
                  params.put("topic", topic);
                  params.put("message", message);
                  try {
                      sub.invokeMethod("onMessage", params, false);
                  } catch (FrameworkException e) {
                      logger.warn("Could not invoke 'onMessage' method on MessageSubscriber: " + e.getMessage());
                  }
              }
          });
        }

        return new RestMethodResult(200);
    }

    @Export
    public RestMethodResult subscribeTopic(final String topic) throws FrameworkException {

        return new RestMethodResult(200);
    }

    @Export
    public RestMethodResult unsubscribeTopic(final String topic) throws FrameworkException {

        return new RestMethodResult(200);
    }

}
