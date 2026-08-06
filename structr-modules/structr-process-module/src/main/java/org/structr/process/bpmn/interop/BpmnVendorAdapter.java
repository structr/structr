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

import org.structr.common.error.FrameworkException;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Set;

/**
 * Pluggable importer for one BPMN vendor's dialect. A competitor engine has to <em>digest
 * and re-use</em> BPMN authored for Camunda, Zeebe, Flowable, and friends; this is the
 * extension point that makes each vendor an isolated, testable plugin instead of a thicket
 * of {@code if (VENDOR_NS...)} branches in {@link org.structr.process.bpmn.BpmnImporter}.
 *
 * <h3>Design pillars (decided; keep to them)</h3>
 * <ol>
 *   <li><b>The Structr graph model is canonical; vendor formats live only at the boundary.</b>
 *       Everything in Structr is a schema type + graph. Adapters translate a foreign dialect
 *       <em>into</em> that model on the way in and never let a foreign model (loose "process
 *       variables", vendor form objects) leak inward. This is the anti-corruption boundary: our
 *       differentiation (graph queries, live schema, unified data + process) depends on the
 *       native model being the only source of truth.</li>
 *   <li><b>Forms are not native BPMN, so we translate, not "read a standard".</b> BPMN's own
 *       data model stops at the whole-item grain ({@code itemDefinition} / {@code ioSpecification})
 *       and cannot express "this task shows these fields of that type". Every engine therefore
 *       invented an extension (Camunda {@code formData/formField}, Zeebe embedded
 *       {@code userTaskForm}, Flowable {@code formProperty}). There is no single element to read;
 *       each adapter speaks one dialect.</li>
 *   <li><b>form-js / Camunda-Form JSON is our canonical form shape -- clean-room.</b> It is the
 *       closest thing to a cross-vendor form schema, so {@link VendorFormField} mirrors it. We
 *       implement our own reader/writer against the (uncopyrightable) <em>format</em>; we do not
 *       vendor the bpmn.io libraries or their renderer (their license carries a watermark
 *       obligation), and we do not use their trademarks.</li>
 *   <li><b>Schema-type-based, always -- so we synthesize.</b> Structr has no type-optional mode;
 *       a process with no subject type cannot render a form. When a foreign model carries forms
 *       but no Structr type, the {@link SubjectTypeSynthesizer} manufactures one from the union
 *       of the adapters' fields. Adapters produce the field model; synthesis makes it a type.</li>
 * </ol>
 *
 * <p>An adapter is stateless and side-effect free: it only reads XML and returns a vendor-neutral
 * model. Graph mutation (schema synthesis, wiring the contract) is the caller's job, so adapters
 * stay trivially unit-testable against a DOM fragment.</p>
 */
public interface BpmnVendorAdapter {

	/** Short, human-readable vendor name for logging (e.g. "Camunda"). Not a trademark claim. */
	String vendorName();

	/**
	 * Does this adapter apply to a document declaring these namespace URIs? Detection is by
	 * namespace so a file mixing dialects (common after tool migrations) can be handled by
	 * several adapters at once.
	 *
	 * @param namespaceUris the set of namespace URIs declared on the document root
	 */
	boolean appliesTo(Set<String> namespaceUris);

	/**
	 * Extract every user-task form found under {@code processEl} (descending into sub-processes),
	 * normalized to Structr terms. Returns an empty list when this dialect declares no forms on
	 * this process -- never null. Must not mutate the graph.
	 *
	 * @param processEl the {@code <bpmn:process>} element
	 */
	List<VendorTaskForm> extractForms(final Element processEl) throws FrameworkException;
}
