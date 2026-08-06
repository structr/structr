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
package org.structr.process.bpmn.interop;

import java.util.List;
import java.util.Set;

/**
 * Registry of the known {@link BpmnVendorAdapter}s. A file can declare several vendor namespaces
 * (common after tool migrations), so {@link #applicableTo} returns every adapter that matches
 * rather than picking one -- their extracted forms are merged by the caller.
 *
 * <p>Ships adapters for Camunda 7 ({@code camunda:formData}), Zeebe / Camunda 8
 * ({@code zeebe:userTaskForm} + form-js JSON) and Flowable / Activiti
 * ({@code flowable:formProperty}). If adapters ever need to ship from other modules, this becomes
 * a service-loaded registry -- the {@link BpmnVendorAdapter} contract is designed to survive that
 * change.</p>
 */
public final class BpmnVendorAdapters {

	private static final List<BpmnVendorAdapter> ALL = List.of(new CamundaFormAdapter(), new ZeebeFormAdapter(), new FlowableFormAdapter());

	private BpmnVendorAdapters() {}

	/** Every adapter that applies to a document declaring these namespace URIs. */
	public static List<BpmnVendorAdapter> applicableTo(final Set<String> namespaceUris) {

		return ALL.stream().filter(a -> a.appliesTo(namespaceUris)).toList();
	}
}
