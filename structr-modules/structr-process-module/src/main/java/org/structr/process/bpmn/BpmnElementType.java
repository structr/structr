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
package org.structr.process.bpmn;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The BPMN 2.0 flow-node / artifact element types the process module recognises,
 * paired with their BPMN XML local names (the value stored in
 * {@code BpmnElement.bpmnElementType}).
 *
 * <p>Central registry for these strings: the importer's whitelist, the engine's
 * {@code advanceToken} switch and the exporter all resolve through this enum
 * instead of re-typing string literals, so the set of understood types is
 * defined in one place and can be switched on exhaustively.</p>
 */
public enum BpmnElementType {

	START_EVENT("startEvent"),
	END_EVENT("endEvent"),
	INTERMEDIATE_THROW_EVENT("intermediateThrowEvent"),
	INTERMEDIATE_CATCH_EVENT("intermediateCatchEvent"),
	BOUNDARY_EVENT("boundaryEvent"),
	USER_TASK("userTask"),
	SERVICE_TASK("serviceTask"),
	SCRIPT_TASK("scriptTask"),
	MANUAL_TASK("manualTask"),
	TASK("task"),
	EXCLUSIVE_GATEWAY("exclusiveGateway"),
	PARALLEL_GATEWAY("parallelGateway"),
	INCLUSIVE_GATEWAY("inclusiveGateway"),
	EVENT_BASED_GATEWAY("eventBasedGateway"),
	SUB_PROCESS("subProcess"),
	CALL_ACTIVITY("callActivity"),
	DATA_OBJECT_REFERENCE("dataObjectReference"),
	DATA_STORE_REFERENCE("dataStoreReference"),
	DATA_OBJECT("dataObject"),
	ASSOCIATION("association"),

	/** Sentinel for an element type the module does not model. */
	UNKNOWN(null);

	private final String bpmnName;

	BpmnElementType(final String bpmnName) {
		this.bpmnName = bpmnName;
	}

	/** The BPMN XML local name for this type ({@code null} for {@link #UNKNOWN}). */
	public String bpmnName() {
		return bpmnName;
	}

	/** True if {@code elementType} is this type's BPMN local name. */
	public boolean matches(final String elementType) {
		return bpmnName != null && bpmnName.equals(elementType);
	}

	/**
	 * Resolve a BPMN local name to its enum constant, or {@link #UNKNOWN} if the
	 * name is null or not one of the recognised types.
	 */
	public static BpmnElementType fromBpmnName(final String elementType) {

		if (elementType != null) {

			for (final BpmnElementType type : values()) {

				if (elementType.equals(type.bpmnName)) {
					return type;
				}
			}
		}
		return UNKNOWN;
	}

	/** True if the given BPMN local name is a recognised element type. */
	public static boolean isKnown(final String elementType) {
		return fromBpmnName(elementType) != UNKNOWN;
	}

	/** The BPMN local names of all recognised types (excludes {@link #UNKNOWN}). */
	public static Set<String> knownTypeNames() {

		return Arrays.stream(values())
			.map(BpmnElementType::bpmnName)
			.filter(name -> name != null)
			.collect(Collectors.toUnmodifiableSet());
	}
}
