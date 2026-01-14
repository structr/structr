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
package org.structr.xmpp;

import org.structr.api.service.LicenseManager;
import org.structr.core.traits.StructrTraits;
import org.structr.module.StructrModule;
import org.structr.xmpp.traits.definitions.XMPPClientTraitDefinition;
import org.structr.xmpp.traits.definitions.XMPPRequestTraitDefinition;
import org.structr.xmpp.traits.relationships.XMPPClientRequest;

import java.util.Set;

/**
 *
 */
public class XMPPModule implements StructrModule {

	@Override
	public void onLoad() {

		StructrTraits.registerTrait(new XMPPClientRequest());
		StructrTraits.registerRelationshipType(StructrTraits.XMPP_CLIENT_REQUEST, StructrTraits.XMPP_CLIENT_REQUEST);

		StructrTraits.registerTrait(new XMPPClientTraitDefinition());
		StructrTraits.registerTrait(new XMPPRequestTraitDefinition());

		StructrTraits.registerNodeType(StructrTraits.XMPP_CLIENT,  StructrTraits.XMPP_CLIENT);
		StructrTraits.registerNodeType(StructrTraits.XMPP_REQUEST, StructrTraits.XMPP_REQUEST);
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {
	}

	@Override
	public String getName() {
		return "xmpp";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
