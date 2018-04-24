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
package org.structr.flow.impl;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.flow.api.DataSource;
import org.structr.flow.engine.Context;
import org.structr.flow.impl.rels.FlowDataInput;

import java.util.List;

public class FlowParameterDataSource extends FlowBaseNode implements DataSource {

    public static final Property<List<FlowBaseNode>> dataTarget 	= new EndNodes<>("dataTarget", FlowDataInput.class);
    public static final Property<String> key             		    = new StringProperty("key");

    public static final View defaultView 						    = new View(FlowDataSource.class, PropertyView.Public, key, dataTarget);
    public static final View uiView      						    = new View(FlowDataSource.class, PropertyView.Ui, key, dataTarget);

    @Override
    public Object get(Context context) {
        String _key = getProperty(key);
        if (_key != null) {
           return context.getParameter(_key);
        }
        return null;
    }
}
