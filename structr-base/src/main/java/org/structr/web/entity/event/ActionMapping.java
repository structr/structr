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
package org.structr.web.entity.event;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;

public interface ActionMapping extends NodeTrait {

	String getEvent();
	String getAction();
	String getMethod();
	String getDataType();
	String getIdExpression();
	String getOptions();

	String getDialogType();
	String getDialogTitle();
	String getDialogText();

	Iterable<ParameterMapping> getParameterMappings();
	Iterable<DOMElement> getTriggerElements();
	Iterable<DOMNode> getSuccessTargets();
	Iterable<DOMNode> getFailureTargets();

	String getSuccessNotifications();
	String getSuccessBehaviour();
	String getSuccessPartial();
	String getSuccessURL();
	String getSuccessEvent();
	String getSuccessNotificationsPartial();
	String getSuccessNotificationsEvent();
	Integer getSuccessNotificationsDelay();

	String getFailureNotifications();
	String getFailureBehaviour();
	String getFailurePartial();
	String getFailureURL();
	String getFailureEvent();
	String getFailureNotificationsPartial();
	String getFailureNotificationsEvent();
	Integer getFailureNotificationsDelay();
}
