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
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.files.url.StructrURLStreamHandlerFactory;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;
import org.structr.web.datasource.CypherGraphDataSource;
import org.structr.web.datasource.FunctionDataSource;
import org.structr.web.datasource.IdRequestParameterGraphDataSource;
import org.structr.web.datasource.RestDataSource;
import org.structr.web.function.*;
import org.structr.web.traits.definitions.*;
import org.structr.web.traits.definitions.dom.*;
import org.structr.web.traits.definitions.html.*;
import org.structr.web.traits.relationships.*;

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
		StructrTraits.registerRelationshipType("DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping",          new DOMNodeFAILURE_NOTIFICATION_ELEMENTActionMapping());
		StructrTraits.registerRelationshipType("DOMNodeSUCCESS_TARGETActionMapping",                        new DOMNodeSUCCESS_TARGETActionMapping());
		StructrTraits.registerRelationshipType("DOMNodeFAILURE_TARGETActionMapping",                        new DOMNodeFAILURE_TARGETActionMapping());
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
		StructrTraits.registerNodeType("Comment",                          new DOMNodeTraitDefinition(), new ContentTraitDefinition(), new CommentTraitDefinition());
		StructrTraits.registerNodeType("Content",                          new DOMNodeTraitDefinition(), new ContentTraitDefinition());
		StructrTraits.registerNodeType("CssDeclaration",                   new CssDeclarationTraitDefinition());
		StructrTraits.registerNodeType("CssRule",                          new CssRuleTraitDefinition());
		StructrTraits.registerNodeType("CssSelector",                      new CssSelectorTraitDefinition());
		StructrTraits.registerNodeType("CssSemanticClass",                 new CssSemanticClassTraitDefinition());
		StructrTraits.registerNodeType("CssDeclaration",                   new CssDeclarationTraitDefinition());
		StructrTraits.registerNodeType("DOMNode",                          new DOMNodeTraitDefinition());
		StructrTraits.registerNodeType("DOMElement",                       new DOMNodeTraitDefinition(), new DOMElementTraitDefinition());
		StructrTraits.registerNodeType("File",                             new AbstractFileTraitDefinition(), new FileTraitDefinition(), new LinkableTraitDefinition());
		StructrTraits.registerNodeType("Image",                            new AbstractFileTraitDefinition(), new FileTraitDefinition(), new ImageTraitDefinition(), new LinkableTraitDefinition());
		StructrTraits.registerNodeType("Folder",                           new AbstractFileTraitDefinition(), new FolderTraitDefinition());
		StructrTraits.registerNodeType("Linkable",                         new LinkableTraitDefinition());
		StructrTraits.registerNodeType("LinkSource",                       new LinkSourceTraitDefinition());
		StructrTraits.registerNodeType("Page",                             new DOMNodeTraitDefinition(), new LinkableTraitDefinition(), new PageTraitDefinition());
		StructrTraits.registerNodeType("PagePath",                         new PagePathTraitDefinition());
		StructrTraits.registerNodeType("PagePathParameter",                new PagePathParameterTraitDefinition());
		StructrTraits.registerNodeType("ParameterMapping",                 new ParameterMappingTraitDefinition());
		StructrTraits.registerNodeType("ShadowDocument",                   new DOMNodeTraitDefinition(), new LinkableTraitDefinition(), new PageTraitDefinition(), new ShadowDocumentTraitDefinition());
		StructrTraits.registerNodeType("Site",                             new SiteTraitDefinition());
		StructrTraits.registerNodeType("StorageConfiguration",             new StorageConfigurationTraitDefinition());
		StructrTraits.registerNodeType("StorageConfigurationEntry",        new StorageConfigurationEntryTraitDefinition());
		StructrTraits.registerNodeType("Template",                         new DOMNodeTraitDefinition(), new ContentTraitDefinition(), new TemplateTraitDefinition());
		StructrTraits.registerNodeType("User",                             new PrincipalTraitDefinition(), new UserTraitDefinition());
		StructrTraits.registerNodeType("Widget",                           new WidgetTraitDefinition());



		// HTML elements
		StructrTraits.registerNodeType("Html",  new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Html"));
		StructrTraits.registerNodeType("Head",  new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Head"));
		StructrTraits.registerNodeType("Meta",  new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Meta"));
		StructrTraits.registerNodeType("Body",  new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Body"));
		StructrTraits.registerNodeType("Input", new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Input"));
		StructrTraits.registerNodeType("Div",   new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Div"));
		StructrTraits.registerNodeType("A",     new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("A"));

		StructrTraits.registerNodeType("A",               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new A());
		StructrTraits.registerNodeType("Abbr",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Abbr());
		StructrTraits.registerNodeType("Address",         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Address"));
		StructrTraits.registerNodeType("Area",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Area());
		StructrTraits.registerNodeType("Article",         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Article"));
		StructrTraits.registerNodeType("Aside",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Aside"));
		StructrTraits.registerNodeType("Audio",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Audio());
		StructrTraits.registerNodeType("Base",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Base());
		StructrTraits.registerNodeType("Bdi",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Bdi());
		StructrTraits.registerNodeType("Bdo",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Bdo());
		StructrTraits.registerNodeType("B",               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new B());
		StructrTraits.registerNodeType("Blockquote",      new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Blockquote"));
		StructrTraits.registerNodeType("Body",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Body"));
		StructrTraits.registerNodeType("Br",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Br());
		StructrTraits.registerNodeType("Button",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Button());
		StructrTraits.registerNodeType("Canvas",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Canvas());
		StructrTraits.registerNodeType("Caption",         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Caption"));
		StructrTraits.registerNodeType("Cite",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Cite());
		StructrTraits.registerNodeType("Code",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Code());
		StructrTraits.registerNodeType("Colgroup",        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Colgroup"));
		StructrTraits.registerNodeType("Col",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Col());
		StructrTraits.registerNodeType("Command",         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Command());
		StructrTraits.registerNodeType("Data",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Data());
		StructrTraits.registerNodeType("Datalist",        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Datalist"));
		StructrTraits.registerNodeType("Dd",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Dd"));
		StructrTraits.registerNodeType("Del",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Del"));
		StructrTraits.registerNodeType("Details",         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Details());
		StructrTraits.registerNodeType("Dfn",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Dfn());
		StructrTraits.registerNodeType("Dialog",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Dialog());
		StructrTraits.registerNodeType("Div",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Div"));
		StructrTraits.registerNodeType("Dl",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Dl"));
		StructrTraits.registerNodeType("Dt",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Dt"));
		StructrTraits.registerNodeType("Embed",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Embed());
		StructrTraits.registerNodeType("Em",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Em());
		StructrTraits.registerNodeType("Fieldset",        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Fieldset"));
		StructrTraits.registerNodeType("Figcaption",      new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Figcaption"));
		StructrTraits.registerNodeType("Figure",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Figure"));
		StructrTraits.registerNodeType("Footer",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Footer"));
		StructrTraits.registerNodeType("Form",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Form());
		StructrTraits.registerNodeType("G",               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new G());
		StructrTraits.registerNodeType("H1",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("H1"));
		StructrTraits.registerNodeType("H2",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("H2"));
		StructrTraits.registerNodeType("H3",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("H3"));
		StructrTraits.registerNodeType("H4",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("H4"));
		StructrTraits.registerNodeType("H5",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("H5"));
		StructrTraits.registerNodeType("H6",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("H6"));
		StructrTraits.registerNodeType("Header",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Header"));
		StructrTraits.registerNodeType("Head",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Head"));
		StructrTraits.registerNodeType("Hgroup",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Hgroup"));
		StructrTraits.registerNodeType("Hr",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Hr());
		StructrTraits.registerNodeType("Html",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Html());
		StructrTraits.registerNodeType("Iframe",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Iframe());
		StructrTraits.registerNodeType("I",               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new I());
		StructrTraits.registerNodeType("Img",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new Img());
		StructrTraits.registerNodeType("Input",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Input());
		StructrTraits.registerNodeType("Ins",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Ins"));
		StructrTraits.registerNodeType("Kbd",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Kbd());
		StructrTraits.registerNodeType("Keygen",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Keygen());
		StructrTraits.registerNodeType("Label",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Label());
		StructrTraits.registerNodeType("Legend",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Legend"));
		StructrTraits.registerNodeType("Li",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Li());
		StructrTraits.registerNodeType("Link",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new Link());
		StructrTraits.registerNodeType("Main",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Main"));
		StructrTraits.registerNodeType("Map",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Map"));
		StructrTraits.registerNodeType("Mark",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Mark());
		StructrTraits.registerNodeType("Menu",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Menu"));
		StructrTraits.registerNodeType("Meta",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Meta());
		StructrTraits.registerNodeType("Meter",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Meter"));
		StructrTraits.registerNodeType("Nav",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Nav"));
		StructrTraits.registerNodeType("Noscript",        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Noscript"));
		StructrTraits.registerNodeType("Object",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Object"));
		StructrTraits.registerNodeType("Ol",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Ol());
		StructrTraits.registerNodeType("Optgroup",        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Optgroup());
		StructrTraits.registerNodeType("Option",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Option());
		StructrTraits.registerNodeType("Output",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Output"));
		StructrTraits.registerNodeType("Param",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Param());
		StructrTraits.registerNodeType("Picture",         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Picture"));
		StructrTraits.registerNodeType("P",               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("P"));
		StructrTraits.registerNodeType("Pre",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Pre());
		StructrTraits.registerNodeType("Progress",        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Progress"));
		StructrTraits.registerNodeType("Q",               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Q());
		StructrTraits.registerNodeType("Rp",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Rp());
		StructrTraits.registerNodeType("Rt",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Rt());
		StructrTraits.registerNodeType("Ruby",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Ruby());
		StructrTraits.registerNodeType("Samp",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Samp());
		StructrTraits.registerNodeType("Script",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new Script());
		StructrTraits.registerNodeType("Section",         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Section"));
		StructrTraits.registerNodeType("Select",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Select());
		StructrTraits.registerNodeType("S",               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new S());
		StructrTraits.registerNodeType("Slot",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Slot());
		StructrTraits.registerNodeType("Small",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Small());
		StructrTraits.registerNodeType("Source",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Source());
		StructrTraits.registerNodeType("Span",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Span());
		StructrTraits.registerNodeType("Strong",          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Strong());
		StructrTraits.registerNodeType("Style",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Style());
		StructrTraits.registerNodeType("Sub",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Sub());
		StructrTraits.registerNodeType("Summary",         new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Summary"));
		StructrTraits.registerNodeType("Sup",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Sup());
		StructrTraits.registerNodeType("Table",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Table"));
		StructrTraits.registerNodeType("Tbody",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Tbody"));
		StructrTraits.registerNodeType("Td",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Td());
		StructrTraits.registerNodeType("TemplateElement", new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new TemplateElement());
		StructrTraits.registerNodeType("Textarea",        new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Textarea());
		StructrTraits.registerNodeType("Tfoot",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Tfoot"));
		StructrTraits.registerNodeType("Thead",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Thead"));
		StructrTraits.registerNodeType("Th",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Th());
		StructrTraits.registerNodeType("Time",            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Time());
		StructrTraits.registerNodeType("Title",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Title"));
		StructrTraits.registerNodeType("Track",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Track());
		StructrTraits.registerNodeType("Tr",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Tr"));
		StructrTraits.registerNodeType("U",               new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new U());
		StructrTraits.registerNodeType("Ul",              new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new GenericHtmlElementTraitDefinition("Ul"));
		StructrTraits.registerNodeType("Var",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Var());
		StructrTraits.registerNodeType("Video",           new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Video());
		StructrTraits.registerNodeType("Wbr",             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new Wbr());

		/*
		StructrTraits.registerNodeType("A",                                new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new LinkSourceTraitDefinition(), new ATraitDefinition());
		StructrTraits.registerNodeType("Abbr",                             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new AbbrTraitDefinition());
		StructrTraits.registerNodeType("Address",                          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new AddressTraitDefinition());
		StructrTraits.registerNodeType("Area",                             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new AreaTraitDefinition());
		StructrTraits.registerNodeType("Article",                          new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new ArticleTraitDefinition());
		StructrTraits.registerNodeType("Aside",                            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new AsideTraitDefinition());
		StructrTraits.registerNodeType("Audio",                            new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new AudioTraitDefinition());
		StructrTraits.registerNodeType("B",                                new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new BTraitDefinition());
		StructrTraits.registerNodeType("Base",                             new DOMNodeTraitDefinition(), new DOMElementTraitDefinition(), new BaseTraitDefinition());
		*/
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
		return Set.of("rest");
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
