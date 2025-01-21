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
package org.structr.process.entity.relationship;

import org.structr.core.entity.ManyToMany;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.event.ActionMapping;

public class DOMNodePROCESS_SHOW_ELEMENT_ON_SUCCESSActionMapping extends ManyToMany<DOMNode, ActionMapping> {

    @Override
    public Class<DOMNode> getSourceType() {
        return DOMNode.class;
    }

    @Override
    public Class<ActionMapping> getTargetType() {
        return ActionMapping.class;
    }

    @Override
    public String name() {
        return "PROCESS_SHOW_ELEMENT_ON_SUCCESS";
    }
}