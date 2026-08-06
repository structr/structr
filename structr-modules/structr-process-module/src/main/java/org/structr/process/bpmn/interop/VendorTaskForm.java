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

/**
 * A single user task's form, extracted from a foreign vendor's BPMN and normalized into
 * Structr terms by a {@link BpmnVendorAdapter}.
 *
 * <p>The task is identified by its BPMN id (not a node reference) because extraction happens
 * against the raw XML, before the caller resolves ids to imported {@code BpmnElement} nodes via
 * the element map. The {@link SubjectTypeSynthesizer} does that resolution when it turns each
 * task form into a per-step {@code SchemaView} and wires the step's {@code subjectFormView}.</p>
 *
 * @param taskBpmnId the {@code id} attribute of the {@code <bpmn:userTask>}
 * @param fields     the form's fields, in declared order
 */
public record VendorTaskForm(String taskBpmnId, List<VendorFormField> fields) {}
