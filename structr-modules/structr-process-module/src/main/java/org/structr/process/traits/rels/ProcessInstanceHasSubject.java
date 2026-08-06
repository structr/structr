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
package org.structr.process.traits.rels;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * ProcessInstance -[HAS_SUBJECT]-> NodeInterface (many-to-one).
 * Links a running process instance to the single domain object it operates on
 * (e.g. a LeaveRequest, an Invoice, a LoanApplication). The process instance
 * holds the reference; the subject is the user's domain data, typed via the
 * schema. Subjects typically outlive the process.
 *
 * <p>Deliberately generic (target NodeInterface) so any custom type can serve
 * as a subject without modifying the process engine.</p>
 *
 * <p>Many-to-one: each process instance has at most one subject, but a single
 * domain object can be the subject of multiple concurrent process instances
 * (e.g. a LeaveRequest going through both an approval process and an audit
 * process). For batch operations ("process this list of items"), the engine
 * convention is one process instance per item -- callers loop and invoke
 * startProcess once per subject.</p>
 *
 * <p><b>Read propagation: Out / Add. Write, delete, accessControl:
 * Out / Keep.</b> Engine grants give the initiator and task
 * participants read on the {@code ProcessInstance}; outbound read
 * propagation through this relationship extends that read to the
 * subject. That makes the engine-managed access model (&#167;11.17)
 * cover subjects too: a participant who can see the instance
 * automatically sees its subject, with no per-schema ACL setup
 * needed. Write / delete / accessControl are {@code Keep} (no-op:
 * the mask passes through unchanged), so the initiator's owner-level
 * perms on the instance (R+W+D+AC because {@code startProcess} runs
 * under their context) do not propagate to the subject -- only the
 * read bit, which we explicitly {@code Add}, flows. Direct grants on
 * the subject itself -- schema-level grants, owner relationships
 * authored independently -- are unaffected.</p>
 *
 * <p>For subjects shared across multiple instances, read access stacks
 * naturally from each instance the user participates in -- no
 * double-counting, no leaks: participants in unrelated instances of
 * the same subject don't see each other's instances.</p>
 */
public class ProcessInstanceHasSubject extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ProcessInstanceHasSubject() {

		super(ProcessTraits.PROCESS_INSTANCE_HAS_SUBJECT);
	}

	@Override
	public String getSourceType() {

		return ProcessTraits.PROCESS_INSTANCE;
	}

	@Override
	public String getTargetType() {

		return StructrTraits.NODE_INTERFACE;
	}

	@Override
	public String getRelationshipType() {

		return "HAS_SUBJECT";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {

		return Relation.Multiplicity.Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {

		return Relation.Multiplicity.One;
	}

	@Override
	public int getCascadingDeleteFlag() {

		return Relation.NONE;
	}

	@Override
	public int getAutocreationFlag() {

		return Relation.NONE;
	}

	@Override
	public boolean isInternal() {

		return false;
	}

	@Override
	public PropagationDirection getPropagationDirection() {

		return PropagationDirection.Out;
	}

	@Override
	public PropagationMode getReadPropagation() {

		return PropagationMode.Add;
	}

	@Override
	public PropagationMode getWritePropagation() {

		return PropagationMode.Keep;
	}

	@Override
	public PropagationMode getDeletePropagation() {

		return PropagationMode.Keep;
	}

	@Override
	public PropagationMode getAccessControlPropagation() {

		return PropagationMode.Keep;
	}

	@Override
	public String getDeltaProperties() {

		return null;
	}
}
