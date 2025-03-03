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
package org.structr.core.traits;

import org.structr.core.traits.definitions.*;

public class StructrTraits {

	// node types
	public static final String PROPERTY_CONTAINER       = "PropertyContainer";
	public static final String GRAPH_OBJECT             = "GraphObject";
	public static final String NODE_INTERFACE           = "NodeInterface";
	public static final String RELATIONSHIP_INTERFACE   = "RelationshipInterface";
	public static final String ACCESS_CONTROLLABLE      = "AccessControllable";
	public static final String PRINCIPAL                = "Principal";
	public static final String USER                     = "User";
	public static final String GROUP                    = "Group";
	public static final String ABSTRACT_FILE            = "AbstractFile";
	public static final String FOLDER                   = "Folder";
	public static final String FILE                     = "File";
	public static final String IMAGE                    = "Image";
	public static final String ABSTRACT_SCHEMA_NODE     = "AbstractSchemaNode";
	public static final String SCHEMA_NODE              = "SchemaNode";
	public static final String SCHEMA_PROPERTY          = "SchemaProperty";
	public static final String SCHEMA_METHOD            = "SchemaMethod";
	public static final String SCHEMA_VIEW              = "SchemaView";
	public static final String SCHEMA_GRANT             = "SchemaGrant";
	public static final String SCHEMA_RELATIONSHIP_NODE = "SchemaRelationshipNode";
	public static final String SCHEMA_NODE_METHOD       = "SchemaNodeMethod";
	public static final String LINK_SOURCE              = "LinkSource";
	public static final String LINKABLE                 = "Linkable";
	public static final String DOM_NODE                 = "DOMNode";
	public static final String DOM_ELEMENT              = "DOMElement";
	public static final String SHADOW_DOCUMENT          = "ShadowDocument";
	public static final String PAGE                     = "Page";
	public static final String PAGE_PATH                = "PagePath";
	public static final String PAGE_PATH_PARAMETER      = "PagePathParameter";
	public static final String TEMPLATE                 = "Template";
	public static final String CONTENT                  = "Content";
	public static final String ACTION_MAPPING           = "ActionMapping";
	public static final String PARAMETER_MAPPING        = "ParameterMapping";
	public static final String LOCALIZATION             = "Localization";
	public static final String LOCATION                 = "Location";
	public static final String MAIL_TEMPLATE            = "MailTemplate";
	public static final String PERSON                   = "Person";
	public static final String SESSION_DATA_NODE        = "SessionDataNode";
	public static final String SCHEMA_METHOD_PARAMETER  = "SchemaMethodParameter";
	public static final String CORS_SETTING             = "CorsSetting";
	public static final String RESOURCE_ACCESS          = "ResourceAccess";
	public static final String DYNAMIC_RESOURCE_ACCESS  = "DynamicResourceAccess";
	public static final String EMAIL_MESSAGE            = "EMailMessage";
	public static final String MAILBOX                  = "Mailbox";
	public static final String WIDGET                   = "Widget";

	// relationship types
	public static final String SECURITY                              = "Security";
	public static final String PRINCIPAL_OWNS_NODE                   = "PrincipalOwnsNode";
	public static final String PRINCIPAL_SCHEMA_GRANT_RELATIONSHIP   = "PrincipalSchemaGrantRelationship";
	public static final String GROUP_CONTAINS_PRINCIPAL              = "GroupCONTAINSPrincipal";
	public static final String SCHEMA_EXCLUDED_VIEW_PROPERTY         = "SchemaExcludedViewProperty";
	public static final String SCHEMA_GRANT_SCHEMA_NODE_RELATIONSHIP = "SchemaGrantSchemaNodeRelationship";
	public static final String SCHEMA_METHOD_PARAMETERS              = "SchemaMethodParameters";
	public static final String SCHEMA_NODE_EXTENDS_SCHEMA_NODE       = "SchemaNodeExtendsSchemaNode";
	public static final String SCHEMA_NODE_PROPERTY                  = "SchemaNodeProperty";
	public static final String SCHEMA_NODE_VIEW                      = "SchemaNodeView";
	public static final String SCHEMA_RELATIONSHIP_SOURCE_NODE       = "SchemaRelationshipSourceNode";
	public static final String SCHEMA_RELATIONSHIP_TARGET_NODE       = "SchemaRelationshipTargetNode";
	public static final String SCHEMA_VIEW_PROPERTY                  = "SchemaViewProperty";

	public static void registerBaseType(final TraitDefinition definition) {

		final Traits traits = new TraitsImplementation(definition.getName(), true, false, false, false, false);

		traits.registerImplementation(definition, false);
	}

	public static void registerNodeInterface() {

		final Traits traits = new TraitsImplementation(StructrTraits.NODE_INTERFACE, true, true, false, false, false);

		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
	}

	public static void registerRelationshipInterface() {

		final Traits traits = new TraitsImplementation(StructrTraits.RELATIONSHIP_INTERFACE, true, false, true, false, false);

		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);
	}

	public static void registerDynamicNodeType(final String typeName, final boolean changelogEnabled, final boolean isServiceClass, final TraitDefinition... definitions) {

		Traits traits;

		// do not overwrite types
		if (Traits.getAllTypes(null).contains(typeName)) {

			// caution: this might return a relationship type..
			traits = Traits.of(typeName);

		} else {

			traits = new TraitsImplementation(typeName, false, true, false, changelogEnabled, isServiceClass);

			// Node types consist of at least the following traits
			traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
			traits.registerImplementation(new GraphObjectTraitDefinition(), false);
			traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
			traits.registerImplementation(new AccessControllableTraitDefinition(), false);
		}

		// add implementation (allow extension of existing types)
		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, true);
		}
	}

	public static void registerDynamicRelationshipType(final String typeName, final boolean changelogEnabled, final TraitDefinition... definitions) {

		// do not overwrite types
		if (Traits.getAllTypes(null).contains(typeName)) {
			return;
		}

		final Traits traits = new TraitsImplementation(typeName, false, false, true, changelogEnabled, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, true);
		}
	}

	public static void registerNodeType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new TraitsImplementation(typeName, true, true, false, false, false);

		// Node types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new NodeInterfaceTraitDefinition(), false);
		traits.registerImplementation(new AccessControllableTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, false);
		}
	}

	public static void registerRelationshipType(final String typeName, final TraitDefinition... definitions) {

		final Traits traits = new TraitsImplementation(typeName, true, false, true, false, false);

		// Relationship types consist of at least the following traits
		traits.registerImplementation(new PropertyContainerTraitDefinition(), false);
		traits.registerImplementation(new GraphObjectTraitDefinition(), false);
		traits.registerImplementation(new RelationshipInterfaceTraitDefinition(), false);

		for (final TraitDefinition definition : definitions) {
			traits.registerImplementation(definition, false);
		}
	}
}
