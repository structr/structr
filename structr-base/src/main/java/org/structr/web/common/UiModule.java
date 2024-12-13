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
package org.structr.web.common;

import org.structr.api.service.LicenseManager;
import org.structr.core.datasources.DataSources;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.core.traits.StructrTraits;
import org.structr.files.url.StructrURLStreamHandlerFactory;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;
import org.structr.web.datasource.*;
import org.structr.web.traits.relationships.SiteCONTAINSPage;
import org.structr.web.traits.relationships.PageHAS_PATHPagePath;
import org.structr.web.traits.relationships.DOMNodeSYNCDOMNode;
import org.structr.web.traits.relationships.DOMNodeSUCCESS_TARGETActionMapping;
import org.structr.web.traits.relationships.DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping;
import org.structr.web.traits.relationships.DOMNodePAGEPage;
import org.structr.web.traits.relationships.DOMNodeFAILURE_TARGETActionMapping;
import org.structr.web.traits.relationships.DOMNodeCONTAINSDOMNode;
import org.structr.web.traits.relationships.DOMNodeCONTAINS_NEXT_SIBLINGDOMNode;
import org.structr.web.traits.relationships.DOMElementTRIGGERED_BYActionMapping;
import org.structr.web.traits.relationships.DOMElementRELOADSDOMElement;
import org.structr.web.traits.relationships.DOMElementINPUT_ELEMENTParameterMapping;
import org.structr.web.traits.relationships.ActionMappingPARAMETERParameterMapping;
import org.structr.web.traits.relationships.PagePathHAS_PARAMETERPagePathParameter;
import org.structr.web.traits.relationships.*;
import org.structr.web.function.*;
import org.structr.web.traits.definitions.*;

import java.net.URL;
import java.util.Set;

/**
 */
public class UiModule implements StructrModule {

	static {

		URL.setURLStreamHandlerFactory(new StructrURLStreamHandlerFactory());
	}

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		DataSources.put(true, "ui", "idRequestParameterDataSource", new IdRequestParameterGraphDataSource("nodeId"));
		DataSources.put(true, "ui", "restDataSource",               new RestDataSource());
		DataSources.put(true, "ui", "cypherDataSource",             new CypherGraphDataSource());
		DataSources.put(true, "ui", "functionDataSource",           new FunctionDataSource("functionQuery"));

		StructrTraits.registerRelationshipType("AbstractFileCONFIGURED_BYStorageConfiguration",             new AbstractFileCONFIGURED_BYStorageConfiguration());
		StructrTraits.registerRelationshipType("ActionMappingPARAMETERParameterMapping",                    new ActionMappingPARAMETERParameterMapping());
		StructrTraits.registerRelationshipType("CssRuleCONTAINSCssRule",                                    new CssRuleCONTAINSCssRule());
		StructrTraits.registerRelationshipType("CssRuleHAS_DECLARATIONCssDeclaration",                      new CssRuleHAS_DECLARATIONCssDeclaration());
		StructrTraits.registerRelationshipType("CssRuleHAS_SELECTORCssSelector",                            new CssRuleHAS_SELECTORCssSelector());
		StructrTraits.registerRelationshipType("CssSemanticClassMAPS_TOCssSelector",                        new CssSemanticClassMAPS_TOCssSelector());
		StructrTraits.registerRelationshipType("DOMElementINPUT_ELEMENTParameterMapping",                   new DOMElementINPUT_ELEMENTParameterMapping());
		StructrTraits.registerRelationshipType("DOMElementRELOADSDOMElement",                               new DOMElementRELOADSDOMElement());
		StructrTraits.registerRelationshipType("DOMElementTRIGGERED_BYActionMapping",                       new DOMElementTRIGGERED_BYActionMapping());
		StructrTraits.registerRelationshipType("DOMNodeCONTAINSDOMNode",                                    new DOMNodeCONTAINSDOMNode());
		StructrTraits.registerRelationshipType("DOMNodeCONTAINS_NEXT_SIBLINGDOMNode",                       new DOMNodeCONTAINS_NEXT_SIBLINGDOMNode());
		StructrTraits.registerRelationshipType("DOMNodeFAILURE_TARGETActionMapping",                        new DOMNodeFAILURE_TARGETActionMapping());
		StructrTraits.registerRelationshipType("DOMNodePAGEPage",                                           new DOMNodePAGEPage());
		StructrTraits.registerRelationshipType("DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping",          new DOMNodeSUCCESS_NOTIFICATION_ELEMENTActionMapping());
		StructrTraits.registerRelationshipType("DOMNodeSUCCESS_TARGETActionMapping",                        new DOMNodeSUCCESS_TARGETActionMapping());
		StructrTraits.registerRelationshipType("DOMNodeSYNCDOMNode",                                        new DOMNodeSYNCDOMNode());
		StructrTraits.registerRelationshipType("FolderCONTAINSAbstractFile",                                new FolderCONTAINSAbstractFile());
		StructrTraits.registerRelationshipType("FolderCONTAINSFile",                                        new FolderCONTAINSFile());
		StructrTraits.registerRelationshipType("FolderCONTAINSFolder",                                      new FolderCONTAINSFolder());
		StructrTraits.registerRelationshipType("FolderCONTAINSImage",                                       new FolderCONTAINSImage());
		StructrTraits.registerRelationshipType("ImagePICTURE_OFUser",                                       new ImagePICTURE_OFUser());
		StructrTraits.registerRelationshipType("ImageTHUMBNAILImage",                                       new ImageTHUMBNAILImage());
		StructrTraits.registerRelationshipType("PageHAS_PATHPagePath",                                      new PageHAS_PATHPagePath());
		StructrTraits.registerRelationshipType("LinkSourceLINKLinkable",                                    new LinkSourceLINKLinkableTraitDefinition());
		StructrTraits.registerRelationshipType("PagePathHAS_PARAMETERPagePathParameter",                    new PagePathHAS_PARAMETERPagePathParameter());
		StructrTraits.registerRelationshipType("SiteCONTAINSPage",                                          new SiteCONTAINSPage());
		StructrTraits.registerRelationshipType("StorageConfigurationCONFIG_ENTRYStorageConfigurationEntry", new StorageConfigurationCONFIG_ENTRYStorageConfigurationEntryTraitDefinition());
		StructrTraits.registerRelationshipType("UserHOME_DIRFolder",                                        new UserHOME_DIRFolder());
		StructrTraits.registerRelationshipType("UserWORKING_DIRFolder",                                     new UserWORKING_DIRFolder());

		StructrTraits.registerNodeType("AbstractFile",                     new AbstractFileTraitDefinition());
		StructrTraits.registerNodeType("ActionMapping",                    new ActionMappingTraitDefinition());
		StructrTraits.registerNodeType("ApplicationConfigurationDataNode", new ApplicationConfigurationDataNodeTraitDefinition());
		StructrTraits.registerNodeType("CssDeclaration",                   new CssDeclarationTraitDefinition());
		StructrTraits.registerNodeType("CssRule",                          new CssRuleTraitDefinition());
		StructrTraits.registerNodeType("CssSelector",                      new CssSelectorTraitDefinition());
		StructrTraits.registerNodeType("CssSemanticClass",                 new CssSemanticClassTraitDefinition());
		StructrTraits.registerNodeType("CssDeclaration",                   new CssDeclarationTraitDefinition());
		StructrTraits.registerNodeType("File",                             new AbstractFileTraitDefinition(), new FileTraitDefinition(), new LinkableTraitDefinition());
		StructrTraits.registerNodeType("Image",                            new AbstractFileTraitDefinition(), new FileTraitDefinition(), new ImageTraitDefinition(), new LinkableTraitDefinition());
		StructrTraits.registerNodeType("Folder",                           new AbstractFileTraitDefinition(), new FolderTraitDefinition());
		StructrTraits.registerNodeType("Linkable",                         new LinkableTraitDefinition());
		StructrTraits.registerNodeType("LinkSource",                       new LinkSourceTraitDefinition());
		StructrTraits.registerNodeType("PagePath",                         new PagePathTraitDefinition());
		StructrTraits.registerNodeType("PagePathParameter",                new PagePathParameterTraitDefinition());
		StructrTraits.registerNodeType("ParameterMapping",                 new ParameterMappingTraitDefinition());
		StructrTraits.registerNodeType("Site",                             new SiteTraitDefinition());
		StructrTraits.registerNodeType("StorageConfiguration",             new StorageConfigurationTraitDefinition());
		StructrTraits.registerNodeType("StorageConfigurationEntry",        new StorageConfigurationEntryTraitDefinition());
		StructrTraits.registerNodeType("Widget",                           new WidgetTraitDefinition());
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new EscapeHtmlFunction());
		Functions.put(licenseManager, new EscapeXmlFunction());
		Functions.put(licenseManager, new UnescapeHtmlFunction());
		Functions.put(licenseManager, new StripHtmlFunction());
		Functions.put(licenseManager, new FromJsonFunction());
		Functions.put(licenseManager, new ToJsonFunction());
		Functions.put(licenseManager, new ToGraphObjectFunction());
		Functions.put(licenseManager, new IncludeFunction());
		Functions.put(licenseManager, new IncludeChildFunction());
		Functions.put(licenseManager, new RenderFunction());
		Functions.put(licenseManager, new SetDetailsObjectFunction());
		Functions.put(licenseManager, new ConfirmationKeyFunction());
		Functions.put(licenseManager, new ImportHtmlFunction());
		Functions.put(licenseManager, new ImportCssFunction());
		Functions.put(licenseManager, new RemoveDOMChildFunction());
		Functions.put(licenseManager, new ReplaceDOMChildFunction());
		Functions.put(licenseManager, new InsertHtmlFunction());
		Functions.put(licenseManager, new GetSourceFunction());
		Functions.put(licenseManager, new HasCssClassFunction());

		Functions.put(licenseManager, new SendHtmlMailFunction());
		Functions.put(licenseManager, new SendPlaintextMailFunction());
		Functions.put(licenseManager, new GetContentFunction());
		Functions.put(licenseManager, new SetContentFunction());
		Functions.put(licenseManager, new AppendContentFunction());
		Functions.put(licenseManager, new CopyFileContentsFunction());
		Functions.put(licenseManager, new SetSessionAttributeFunction());
		Functions.put(licenseManager, new GetSessionAttributeFunction());
		Functions.put(licenseManager, new RemoveSessionAttributeFunction());
		Functions.put(licenseManager, new IsLocaleFunction());

		Functions.put(licenseManager, new LogEventFunction());

		Functions.put(licenseManager, new HttpGetFunction());
		Functions.put(licenseManager, new HttpHeadFunction());
		Functions.put(licenseManager, new HttpPatchFunction());
		Functions.put(licenseManager, new HttpPostFunction());
		Functions.put(licenseManager, new HTTPPostMultiPartFunction());
		Functions.put(licenseManager, new HttpPutFunction());
		Functions.put(licenseManager, new HttpDeleteFunction());
		Functions.put(licenseManager, new AddHeaderFunction());
		Functions.put(licenseManager, new ClearHeadersFunction());
		Functions.put(licenseManager, new SetResponseHeaderFunction());
		Functions.put(licenseManager, new RemoveResponseHeaderFunction());
		Functions.put(licenseManager, new SetResponseCodeFunction());
		Functions.put(licenseManager, new GetRequestHeaderFunction());
		Functions.put(licenseManager, new ValidateCertificatesFunction());
		Functions.put(licenseManager, new GetCookieFunction());
		Functions.put(licenseManager, new SetCookieFunction());
		Functions.put(licenseManager, new FromXmlFunction());
		Functions.put(licenseManager, new CreateArchiveFunction());
		Functions.put(licenseManager, new CreateFolderPathFunction());
		Functions.put(licenseManager, new CreateZipFunction());
		Functions.put(licenseManager, new UnarchiveFunction());
		Functions.put(licenseManager, new ScheduleFunction());
		Functions.put(licenseManager, new MaintenanceFunction());
		Functions.put(licenseManager, new BarcodeFunction());
		Functions.put(licenseManager, new JobInfoFunction());
		Functions.put(licenseManager, new JobListFunction());
		Functions.put(licenseManager, new CreateAccessTokenFunction());
		Functions.put(licenseManager, new CreateAccessAndRefreshTokenFunction());

		Functions.put(licenseManager, new ApplicationStorePutFunction());
		Functions.put(licenseManager, new ApplicationStoreDeleteFunction());
		Functions.put(licenseManager, new ApplicationStoreGetFunction());
		Functions.put(licenseManager, new ApplicationStoreGetKeysFunction());
		Functions.put(licenseManager, new ApplicationStoreHasFunction());

		Functions.put(licenseManager, new RequestStorePutFunction());
		Functions.put(licenseManager, new RequestStoreDeleteFunction());
		Functions.put(licenseManager, new RequestStoreGetFunction());
		Functions.put(licenseManager, new RequestStoreGetKeysFunction());
		Functions.put(licenseManager, new RequestStoreHasFunction());

		Functions.put(licenseManager, new SendEventFunction());
		Functions.put(licenseManager, new BroadcastEventFunction());

		Functions.put(licenseManager, new GraphQLFunction());

		Functions.put(licenseManager, new SystemInfoFunction());
	}

	@Override
	public String getName() {
		return "ui";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}
}
