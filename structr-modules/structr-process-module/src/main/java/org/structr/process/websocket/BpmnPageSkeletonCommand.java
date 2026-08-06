/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.websocket;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.traits.StructrTraits;
import org.structr.process.ProcessTraits;
import org.structr.process.bpmn.BpmnPageSkeletonGenerator;
import org.structr.process.entity.BpmnElement;
import org.structr.process.entity.BpmnProcess;
import org.structr.web.entity.Widget;
import org.structr.websocket.command.AbstractCommand;
import org.structr.websocket.message.MessageBuilder;
import org.structr.websocket.message.WebSocketMessage;

import java.util.List;

/**
 * WebSocket command behind the process editor's "Create page skeleton" button: generates an
 * instance page for one BpmnProcess, with one empty div per step that needs a human.
 *
 * <p>Transport only -- the generation itself is {@link BpmnPageSkeletonGenerator}, which is
 * where the behaviour is documented and tested.</p>
 *
 * <pre>
 * { command: 'BPMN_PAGE_SKELETON', data: {
 *     processId:        '&lt;BpmnProcess uuid&gt;',
 *     pageName:         '&lt;optional&gt;',
 *     templateWidgetId: '&lt;optional Widget uuid, isPageTemplate&gt;'
 * } }
 * -&gt; { ok: true, pageId, pageName, stepCount, boundAsInstancePage }
 * </pre>
 */
public class BpmnPageSkeletonCommand extends AbstractCommand {

	public static final String COMMAND_NAME = "BPMN_PAGE_SKELETON";

	@Override
	public void processMessage(final WebSocketMessage webSocketData) throws FrameworkException {

		setDoTransactionNotifications(true);

		final String processId = webSocketData.getNodeDataStringValue("processId");
		if (StringUtils.isEmpty(processId)) {

			throw new FrameworkException(422, "processId is required");
		}

		final BpmnProcess process = getNodeAs(processId, BpmnProcess.class, ProcessTraits.BPMN_PROCESS);
		if (process == null) {

			throw new FrameworkException(422, "BpmnProcess " + processId + " not found");
		}

		// Determined here so "nothing to scaffold" is an error rather than an empty page,
		// and passed on so the generator doesn't walk the graph a second time.
		final List<BpmnElement> steps = BpmnPageSkeletonGenerator.humanFacingSteps(process);
		if (steps.isEmpty()) {

			throw new FrameworkException(422, "This process has no steps that require user interaction, so there is nothing to scaffold.");
		}

		// Optional page-template widget. Resolved here so an unknown id is reported to the user
		// rather than silently producing a bare page. The subject form is not chosen here: the
		// generator uses the canonical Process Subject Form widget, looked up by name.
		final Widget templateWidget = resolveWidget(webSocketData.getNodeDataStringValue("templateWidgetId"), "Page template");
		final BpmnPageSkeletonGenerator.Result result = BpmnPageSkeletonGenerator.createSkeleton(
			StructrApp.getInstance(getWebSocket().getSecurityContext()),
			getWebSocket().getSecurityContext(),
			process,
			steps,
			webSocketData.getNodeDataStringValue("pageName"),
			templateWidget
		);

		getWebSocket().send(MessageBuilder.finished()
			.callback(webSocketData.getCallback())
			.data("ok",                  Boolean.TRUE)
			.data("pageId",              result.pageId())
			.data("pageName",            result.pageName())
			.data("stepCount",           result.stepCount())
			.data("boundAsInstancePage", result.boundAsInstancePage())
			.data("formCount",           result.formCount())
			.data("stepsMissingSubject", result.stepsMissingSubject())
			.build(), true);
	}

	@Override
	public String getCommand() {

		return COMMAND_NAME;
	}

	/** Resolve an optional widget id, or null when none was sent. */
	private Widget resolveWidget(final String widgetId, final String label) throws FrameworkException {

		if (StringUtils.isEmpty(widgetId)) {

			return null;
		}

		final Widget widget = getNodeAs(widgetId, Widget.class, StructrTraits.WIDGET);
		if (widget == null) {

			throw new FrameworkException(422, label + " widget " + widgetId + " not found");
		}

		return widget;
	}
}
