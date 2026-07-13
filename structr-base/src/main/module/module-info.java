/*
 * Copyright (C) 2010-2026 Structr GmbH
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
module structr.base {
    requires com.google.common;
    requires com.google.zxing.javase;
    requires java.sql.rowset;
    requires org.apache.groovy;
    requires org.apache.groovy.json;
    requires org.mongodb.bson;
    requires org.apache.pdfbox;
    requires com.opencsv;
    requires com.twelvemonkeys.common.image;
    requires commons.collections;
    requires cssparser;
    requires diff.match.patch;
    requires flexmark;
    requires flexmark.profile.pegdown;
    requires flexmark.util.ast;
    requires flexmark.util.collection;
    requires flexmark.util.data;
    requires flexmark.util.misc;
    requires flexmark.util.sequence;
    requires jakarta.mail;
    requires java.desktop;
    requires java.management;
    requires java.sql;
    requires jdk.httpserver;
    requires jdk.xml.dom;
    requires jgroups;
    requires jgroups.kubernetes;
    requires jmimemagic;
    requires jwks.rsa;
    requires metadata.extractor;
    requires org.apache.commons.codec;
    requires org.apache.commons.collections4;
    requires org.apache.commons.configuration2;
    requires org.apache.commons.io;
    requires org.apache.commons.text;
    requires org.apache.httpcomponents.httpmime;
    requires org.apache.tika.core;
    requires org.asciidoctor.asciidoctorj.api;
    requires org.bouncycastle.provider;
    requires org.eclipse.elk.core;
    requires org.eclipse.elk.graph;
    requires org.eclipse.elk.graph.json;
    requires org.eclipse.jetty.alpn.server;
    requires org.eclipse.jetty.http;
    requires org.eclipse.jetty.http2.common;
    requires org.eclipse.jetty.http2.server;
    requires org.eclipse.jetty.io;
    requires org.eclipse.jetty.rewrite;
    requires org.eclipse.jetty.websocket.core.server;
    requires org.mongodb.driver.core;
    requires org.mongodb.driver.sync.client;
    requires org.neo4j.driver;
    requires org.shredzone.acme4j;
    requires rest.assured;
    requires sac;
    requires scribejava.apis;
    requires simpleclient.hotspot;
    requires simpleclient.servlet.jakarta;
    requires software.amazon.awssdk.auth;
    requires software.amazon.awssdk.awscore;
    requires software.amazon.awssdk.core;
    requires software.amazon.awssdk.regions;
    requires software.amazon.awssdk.services.s3;
    requires textile.j;
    requires zero.allocation.hashing;
    requires zip4j;

    requires transitive ch.qos.logback.classic;
    requires transitive com.auth0.jwt;
    requires transitive com.google.gson;
    requires transitive com.google.zxing;
    requires transitive jakarta.activation;
    requires transitive jakarta.servlet;
    requires transitive java.logging;
    requires transitive java.xml;
    requires transitive org.apache.commons.compress;
    requires transitive org.apache.commons.lang3;
    requires transitive org.apache.commons.mail;
    requires transitive org.apache.httpcomponents.httpclient;
    requires transitive org.apache.httpcomponents.httpcore;
    requires transitive org.dom4j;
    requires transitive org.eclipse.jetty.ee10.servlet;
    requires transitive org.eclipse.jetty.ee10.servlets;
    requires transitive org.eclipse.jetty.server;
    requires transitive org.eclipse.jetty.session;
    requires transitive org.eclipse.jetty.util;
    requires transitive org.eclipse.jetty.websocket.api;
    requires transitive org.eclipse.jetty.websocket.server;
    requires transitive org.graalvm.polyglot;
    requires transitive org.json;
    requires transitive org.jsoup;
    requires transitive org.slf4j;
    requires transitive scribejava.core;
    requires transitive simpleclient;
    requires transitive structr.db.driver.api;
    requires transitive urlrewritefilter;

    exports org.structr;
    exports org.structr.agent;
    exports org.structr.autocomplete;
    exports org.structr.autocomplete.keywords;
    exports org.structr.common;
    exports org.structr.common.error;
    exports org.structr.common.event;
    exports org.structr.common.fulltext;
    exports org.structr.common.geo;
    exports org.structr.common.helper;
    exports org.structr.common.mail;
    exports org.structr.console;
    exports org.structr.console.shell;
    exports org.structr.console.tabcompletion;
    exports org.structr.core;
    exports org.structr.core.api;
    exports org.structr.core.app;
    exports org.structr.core.auth;
    exports org.structr.core.auth.exception;
    exports org.structr.core.cluster;
    exports org.structr.core.converter;
    exports org.structr.core.cypher;
    exports org.structr.core.datasources;
    exports org.structr.core.datasources.example;
    exports org.structr.core.entity;
    exports org.structr.core.function;
    exports org.structr.core.function.search;
    exports org.structr.core.function.tokenizer;
    exports org.structr.core.graph;
    exports org.structr.core.graph.attribute;
    exports org.structr.core.graph.search;
    exports org.structr.core.notion;
    exports org.structr.core.parser;
    exports org.structr.core.predicate;
    exports org.structr.core.property;
    exports org.structr.core.rest;
    exports org.structr.core.scheduler;
    exports org.structr.core.script;
    exports org.structr.core.script.polyglot;
    exports org.structr.core.script.polyglot.cache;
    exports org.structr.core.script.polyglot.config;
    exports org.structr.core.script.polyglot.context;
    exports org.structr.core.script.polyglot.filesystem;
    exports org.structr.core.script.polyglot.function;
    exports org.structr.core.script.polyglot.util;
    exports org.structr.core.script.polyglot.wrappers;
    exports org.structr.core.traits;
    exports org.structr.core.traits.definitions;
    exports org.structr.core.traits.operations;
    exports org.structr.core.traits.operations.accesscontrollable;
    exports org.structr.core.traits.operations.datasource;
    exports org.structr.core.traits.operations.graphobject;
    exports org.structr.core.traits.operations.nodeinterface;
    exports org.structr.core.traits.operations.principal;
    exports org.structr.core.traits.operations.propertycontainer;
    exports org.structr.core.traits.relationships;
    exports org.structr.core.traits.wrappers;
    exports org.structr.cron;
    exports org.structr.docs;
    exports org.structr.docs.analyzer;
    exports org.structr.docs.documentables.lifecycle;
    exports org.structr.docs.documentables.misc;
    exports org.structr.docs.documentables.service;
    exports org.structr.docs.formatter.json;
    exports org.structr.docs.formatter.markdown;
    exports org.structr.docs.formatter.text;
    exports org.structr.docs.ontology;
    exports org.structr.docs.ontology.parser.rule;
    exports org.structr.docs.ontology.parser.token;
    exports org.structr.files.external;
    exports org.structr.files.sync;
    exports org.structr.files.url;
    exports org.structr.logging;
    exports org.structr.module;
    exports org.structr.module.api;
    exports org.structr.module.xml;
    exports org.structr.rest;
    exports org.structr.rest.adapter;
    exports org.structr.rest.api;
    exports org.structr.rest.api.parameter;
    exports org.structr.rest.auth;
    exports org.structr.rest.common;
    exports org.structr.rest.entity;
    exports org.structr.rest.exception;
    exports org.structr.rest.maintenance;
    exports org.structr.rest.resource;
    exports org.structr.rest.serialization;
    exports org.structr.rest.service;
    exports org.structr.rest.servlet;
    exports org.structr.rest.traits.definitions;
    exports org.structr.rest.traits.relationships;
    exports org.structr.rest.traits.wrappers;
    exports org.structr.schema;
    exports org.structr.schema.action;
    exports org.structr.schema.export;
    exports org.structr.schema.importer;
    exports org.structr.schema.openapi.common;
    exports org.structr.schema.openapi.example;
    exports org.structr.schema.openapi.operation;
    exports org.structr.schema.openapi.operation.maintenance;
    exports org.structr.schema.openapi.parameter;
    exports org.structr.schema.openapi.request;
    exports org.structr.schema.openapi.result;
    exports org.structr.schema.openapi.schema;
    exports org.structr.schema.parser;
    exports org.structr.storage;
    exports org.structr.storage.providers.local;
    exports org.structr.storage.providers.memory;
    exports org.structr.storage.providers.s3;
    exports org.structr.storage.sync;
    exports org.structr.storage.util;
    exports org.structr.util;
    exports org.structr.web;
    exports org.structr.web.agent;
    exports org.structr.web.auth;
    exports org.structr.web.auth.provider;
    exports org.structr.web.common;
    exports org.structr.web.common.microformat;
    exports org.structr.web.converter;
    exports org.structr.web.datasource;
    exports org.structr.web.eam;
    exports org.structr.web.entity;
    exports org.structr.web.entity.css;
    exports org.structr.web.entity.dom;
    exports org.structr.web.entity.event;
    exports org.structr.web.entity.path;
    exports org.structr.web.error;
    exports org.structr.web.function;
    exports org.structr.web.importer;
    exports org.structr.web.maintenance;
    exports org.structr.web.maintenance.deploy;
    exports org.structr.web.property;
    exports org.structr.web.resource;
    exports org.structr.web.schema.parser;
    exports org.structr.web.servlet;
    exports org.structr.web.traits.definitions;
    exports org.structr.web.traits.definitions.dom;
    exports org.structr.web.traits.definitions.html;
    exports org.structr.web.traits.operations;
    exports org.structr.web.traits.relationships;
    exports org.structr.web.traits.wrappers;
    exports org.structr.web.traits.wrappers.dom;
    exports org.structr.websocket;
    exports org.structr.websocket.command;
    exports org.structr.websocket.command.dom;
    exports org.structr.websocket.message;
    exports org.structr.websocket.servlet;


    // ----- service consumers (discovered via ServiceLoader in JarConfigurationProvider) -----
    uses org.structr.module.StructrModule;
    uses org.structr.api.service.Service;
    uses org.structr.agent.Agent;

    // ----- service providers defined by structr-base -----
    provides org.structr.module.StructrModule with
        org.structr.module.CoreModule,
        org.structr.module.AdvancedScriptingModule,
        org.structr.rest.common.RestModule,
        org.structr.web.common.UiModule;

    provides org.structr.api.service.Service with
        org.structr.core.graph.NodeService,
        org.structr.schema.SchemaService,
        org.structr.rest.service.HttpService,
        org.structr.agent.AgentService,
        org.structr.cron.CronService,
        org.structr.files.external.DirectoryWatchService,
        org.structr.files.sync.StorageSyncService;

    provides org.structr.agent.Agent with
        org.structr.web.agent.ThumbnailAgent;
}
