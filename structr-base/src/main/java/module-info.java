/*
 * Copyright (C) 2010-2025 Structr GmbH
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

	requires java.management;
	requires jdk.httpserver;
	requires jdk.xml.dom;
	requires transitive java.compiler;

	requires transitive structr.db.driver.api;
	requires asciidoctor.java.integration;
	requires com.google.common;
	requires com.google.gson;
	requires com.google.zxing.javase;
	requires com.google.zxing;
	requires com.twelvemonkeys.common.image;
	requires commons.collections;
	requires commons.email;
	requires cssparser;
	requires diff.match.patch;
	requires dom4j;
	requires flexmark.profile.pegdown;
	requires flexmark.util.ast;
	requires flexmark.util.data;
	requires flexmark.util.misc;
	requires flexmark;
	requires graphql.java;
	requires java.jwt;
	requires java.xml.bind;
	requires javatools;
	requires javax.mail;
	requires jetty.servlet.api;
	requires jgroups.kubernetes;
	requires jgroups;
	requires jmimemagic;
	requires json.path;
	requires jwks.rsa;
	requires metadata.extractor;
	requires opencsv;
	requires org.antlr.antlr4.runtime;
	requires org.apache.commons.compress;
	requires org.apache.commons.io;
	requires org.apache.httpcomponents.httpclient;
	requires org.apache.httpcomponents.httpcore;
	requires org.apache.httpcomponents.httpmime;
	requires org.apache.oltu.oauth2.client;
	requires org.apache.pdfbox;
	requires org.apache.tika.core;
	requires org.eclipse.elk.core;
	requires org.eclipse.elk.graph;
	requires org.eclipse.elk.graph.json;
	requires org.eclipse.jetty.alpn.server;
	requires org.eclipse.jetty.http2.common;
	requires org.eclipse.jetty.http2.server;
	requires org.eclipse.jetty.io;
	requires org.eclipse.jetty.rewrite;
	requires org.eclipse.jetty.servlet;
	requires org.eclipse.jetty.servlets;
	requires org.eclipse.jetty.util;
	requires org.eclipse.jetty.websocket.jetty.api;
	requires org.eclipse.jetty.websocket.jetty.server;
	requires org.graalvm.polyglot;
	requires org.jsoup;
	requires org.mongodb.bson;
	requires org.mongodb.driver.core;
	requires org.mongodb.driver.sync.client;
	requires org.neo4j.driver;
	requires org.slf4j;
	requires quercus;
	requires rest.assured;
	requires scribejava.apis;
	requires scribejava.core;
	requires simpleclient.hotspot;
	requires simpleclient.servlet.jakarta;
	requires simpleclient;
	requires textile.j;
	requires urlrewritefilter;
	requires xml.apis.ext;
	requires zero.allocation.hashing;
	requires zip4j;
	requires ch.qos.logback.core;
	requires org.apache.commons.configuration2;
	requires commons.lang;
	requires java.sql;
	requires org.apache.commons.collections4;
	requires org.apache.commons.codec;
	requires org.json;
	requires org.apache.commons.text;
	requires org.apache.commons.lang3;
	requires java.desktop;
	requires ch.qos.logback.classic;

	exports org.structr;
	exports org.structr.agent;
	exports org.structr.autocomplete;
	exports org.structr.common;
	exports org.structr.common.error;
	exports org.structr.common.event;
	exports org.structr.common.fulltext;
	exports org.structr.common.geo;
	exports org.structr.common.mail;
	exports org.structr.console;
	exports org.structr.console.rest;
	exports org.structr.console.shell;
	exports org.structr.console.tabcompletion;
	exports org.structr.core;
	exports org.structr.core.app;
	exports org.structr.core.auth;
	exports org.structr.core.auth.exception;
	exports org.structr.core.cluster;
	exports org.structr.core.converter;
	exports org.structr.core.cypher;
	exports org.structr.core.datasources;
	exports org.structr.core.entity;
	exports org.structr.core.function;
	exports org.structr.core.function.search;
	exports org.structr.core.graph;
	exports org.structr.core.graph.attribute;
	exports org.structr.core.graph.search;
	exports org.structr.core.graphql;
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
	exports org.structr.core.script.polyglot.wrappers;
	exports org.structr.cron;
	exports org.structr.files.external;
	exports org.structr.files.url;
	exports org.structr.module;
	exports org.structr.module.api;
	exports org.structr.module.xml;
	exports org.structr.rest;
	exports org.structr.rest.adapter;
	exports org.structr.rest.auth;
	exports org.structr.rest.common;
	exports org.structr.rest.exception;
	exports org.structr.rest.entity;
	exports org.structr.rest.maintenance;
	exports org.structr.rest.resource;
	exports org.structr.rest.serialization;
	exports org.structr.rest.service;
	exports org.structr.rest.servlet;
	exports org.structr.schema;
	exports org.structr.schema.action;
	exports org.structr.schema.compiler;
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
	exports org.structr.web.entity;
	exports org.structr.web.entity.css;
	exports org.structr.web.entity.dom;
	exports org.structr.web.entity.event;
	exports org.structr.web.error;
	exports org.structr.web.function;
	exports org.structr.web.importer;
	exports org.structr.web.maintenance;
	exports org.structr.web.maintenance.deploy;
	exports org.structr.web.property;
	exports org.structr.web.resource;
	exports org.structr.web.schema.parser;
	exports org.structr.web.servlet;
	exports org.structr.websocket;
	exports org.structr.websocket.command;
	exports org.structr.websocket.command.dom;
	exports org.structr.websocket.message;
	exports org.structr.websocket.servlet;
	exports org.structr.core.traits;
	exports org.structr.core.traits.operations;
	exports org.structr.core.traits.operations.graphobject;
	exports org.structr.core.traits.relationships;
	exports org.structr.core.traits.wrappers;
	exports org.structr.core.traits.definitions;
	exports org.structr.rest.traits.relationships;
	exports org.structr.web.traits.definitions;
	exports org.structr.web.traits.wrappers;
	exports org.structr.web.traits.definitions.dom;
	exports org.structr.web.traits.wrappers.dom;
}
