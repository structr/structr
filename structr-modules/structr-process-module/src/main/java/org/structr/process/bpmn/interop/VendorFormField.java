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

/**
 * One field of a foreign vendor's user-task form, normalized into Structr terms.
 *
 * <p>This is the leaf of the vendor-neutral form model produced by a {@link BpmnVendorAdapter}
 * (see that interface for the interop pillar). The shape mirrors a form-js / Camunda-Form
 * component -- {@code key}, {@code label}, a {@code type} -- because that is the closest thing
 * to a cross-vendor form schema; but the {@code type} carried here is already the resolved
 * <em>Structr</em> property type (e.g. "String", "Long", "Boolean", "Date"), because the
 * adapter is the only place allowed to know a foreign type system. Everything downstream
 * (the {@link SubjectTypeSynthesizer}) sees Structr types only.</p>
 *
 * @param key         the field / property name (a valid Structr property name)
 * @param label       human-readable label, or null; carried for future form generation
 * @param structrType the Structr SchemaProperty type ("String" when the vendor type is unknown)
 * @param required    the vendor marked this field required (input-set membership)
 * @param readOnly    the vendor marked this field read-only (excluded from the writable view)
 */
public record VendorFormField(String key, String label, String structrType, boolean required, boolean readOnly) {

	/** The safe default Structr type when a vendor field type has no better mapping. */
	public static final String DEFAULT_TYPE = "String";
}
