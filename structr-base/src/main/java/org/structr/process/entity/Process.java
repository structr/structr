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

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.*;
import org.structr.process.entity.relationship.*;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.event.ActionMapping;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Process extends AbstractNode {

    public static final Property<ProcessState>                  initialState = new EndNode<>("initialState", ProcessINITIAL_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessInstance>> processInstances = new StartNodes<>("processInstances", ProcessInstanceINSTANCE_OFProcess.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessStep>>                steps = new EndNodes<>("steps", ProcessHAS_STEPProcessStep.class).partOfBuiltInSchema();

    public static final View uiView = new View(Process.class, PropertyView.Ui,
            id, name, initialState, steps
    );

    @Export
    public ProcessInstance createInstance(final SecurityContext securityContext) throws FrameworkException {

        // Processes are created by the current user which is also the initial owner
        final App app = StructrApp.getInstance(securityContext);

        // The initial name can and should be changed
        final String initialProcessInstanceName = this.getName() + "-" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());

        final PropertyMap properties = new PropertyMap();
        properties.put(AbstractNode.name, initialProcessInstanceName);
        properties.put(ProcessInstance.process, this);
        properties.put(ProcessInstance.state, this.getProperty(Process.initialState));

        return app.create(ProcessInstance.class, properties);

    }

}
