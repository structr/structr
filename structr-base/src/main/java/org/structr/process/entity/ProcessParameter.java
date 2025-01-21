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
package org.structr.process.entity;

import org.structr.core.entity.AbstractNode;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.StartNode;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.process.entity.relationship.ProcessStateSTATE_PARAMETERProcessParameter;
import org.structr.process.entity.relationship.ProcessStepSTEP_PARAMETERProcessParameter;

public class ProcessParameter extends AbstractNode {

    public static final Property<String>   displayName = new StringProperty("displayName").hint("Display name or label for this parameter").partOfBuiltInSchema();
    public static final Property<String> parameterType = new StringProperty("parameterType").hint("Name of the data type of this parameter, e.g. string, date, boolean etc.").partOfBuiltInSchema();
    public static final Property<Boolean>     required = new BooleanProperty("required").hint("If true, this parameter is mandatory").partOfBuiltInSchema();

    public static final Property<ProcessState>   state = new StartNode<>("state", ProcessStateSTATE_PARAMETERProcessParameter.class).partOfBuiltInSchema();
    public static final Property<ProcessStep>     step = new StartNode<>("step",  ProcessStepSTEP_PARAMETERProcessParameter.class).partOfBuiltInSchema();

}
