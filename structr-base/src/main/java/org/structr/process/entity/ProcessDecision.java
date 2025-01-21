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
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.process.entity.relationship.ProcessDecisionPOSSIBLE_STATEProcessState;
import org.structr.process.entity.relationship.ProcessStepLEADS_TOProcessDecision;

public class ProcessDecision extends AbstractNode {

    public static final Property<String>                      condition = new StringProperty("condition").hint("Condition that has to evaluate to true/false to determine the next process state").partOfBuiltInSchema();
    public static final Property<Iterable<ProcessState>> possibleStates = new EndNodes<>("possibleStates", ProcessDecisionPOSSIBLE_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<ProcessStep>                      step = new StartNode<>("step", ProcessStepLEADS_TOProcessDecision.class).partOfBuiltInSchema();

}
