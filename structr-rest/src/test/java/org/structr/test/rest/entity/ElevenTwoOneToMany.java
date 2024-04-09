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
package org.structr.test.rest.entity;

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.OneToMany;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

public class ElevenTwoOneToMany extends OneToMany<TestEleven, TestTwo> {

    public static final Property<String> startNodeId         = new StringProperty("startNodeId");
    public static final Property<String> endNodeId           = new StringProperty("endNodeId");

    public static final View defaultView = new View(TwoOneOneToMany.class, PropertyView.Public,
            startNodeId, endNodeId
    );

    @Override
    public String name() {
        return "HAS";
    }

    @Override
    public Class getSourceType() {
        return TestEleven.class;
    }

    @Override
    public Class getTargetType() {
        return TestTwo.class;
    }

    @Override
    public Property<String> getSourceIdProperty() {
        return startNodeId;
    }

    @Override
    public Property<String> getTargetIdProperty() {
        return endNodeId;
    }
}
