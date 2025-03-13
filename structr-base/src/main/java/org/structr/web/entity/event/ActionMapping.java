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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;

public interface ActionMapping extends NodeInterface {

	String getEvent();
	String getAction();
	String getMethod();
	String getFlow();
	String getDataType();
	String getIdExpression();
	String getOptions();

	String getDialogType();
	String getDialogTitle();
	String getDialogText();

	void setAction(final String action) throws FrameworkException;
	void setMethod(final String method) throws FrameworkException;
	void setSuccessBehaviour(final String successBehaviour) throws FrameworkException;
	void setFailureBehaviour(final String failureBehaviour) throws FrameworkException;

	Iterable<ParameterMapping> getParameterMappings();
	Iterable<DOMElement> getTriggerElements();
	Iterable<DOMNode> getSuccessTargets();
	Iterable<DOMNode> getFailureTargets();
	Iterable<DOMNode> getSuccessNotificationElements();
	Iterable<DOMNode> getFailureNotificationElements();

	Process     process     getProcess();
	ProcessStep processStep getProcessStep();
	Iterable<DOMNode> processSuccessShowElements getProcessSuccessShowElements();
	Iterable<DOMNode> processSuccessHideElements getProcessSuccessHideElements();
	Iterable<DOMNode> processFailureShowElements getProcessFailureShowElements();
	Iterable<DOMNode> processFailureHideElements getProcessFailureHideElements();
	
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
