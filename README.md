# Structr

[![pipeline status](https://gitlab.structr.com/structr/structr/badges/main/pipeline.svg)](https://gitlab.structr.com/structr/structr/-/commits/main)
[![Hits-of-Code](https://hitsofcode.com/github/structr/structr?branch=main)](https://hitsofcode.com/github/structr/structr/view?branch=main)
![Docker Pulls](https://img.shields.io/docker/pulls/structr/structr)
[![publiccode.yml](https://img.shields.io/badge/publiccode.yml-present-brightgreen)](publiccode.yml)

![License: AGPL v3 / GPL v3](https://img.shields.io/badge/license-AGPLv3%20%2F%20GPLv3-blue.svg)
![Java](https://img.shields.io/badge/Java-GraalVM%20JDK%2025-orange)
![Graph DB](https://img.shields.io/badge/Database-Graph%20(Neo4j)-brightgreen)
![Low Code](https://img.shields.io/badge/Low--Code-Platform-blueviolet)
![Polyglot](https://img.shields.io/badge/Scripting-Polyglot%20(GraalVM)-purple)
![REST API](https://img.shields.io/badge/API-REST%20%2F%20JSON-informational)
![JSON%20Schema](https://img.shields.io/badge/Schema-JSON%20Schema-blue)
![Event Driven](https://img.shields.io/badge/Integration-Event--Driven-yellow)
![FTP](https://img.shields.io/badge/File%20Access-FTP-lightgrey)
![SSH](https://img.shields.io/badge/File%20Access-SSH%20%2F%20SCP-lightgrey)
![AI Ready](https://img.shields.io/badge/AI-MCP%20Enabled-ff69b4)
![GDPR](https://img.shields.io/badge/GDPR-Compliant-success)
![EU Based](https://img.shields.io/badge/Hosting-EU%20%2F%20Germany-blue)

---

## Overview

**Structr** is an open source **generic low-code development and runtime platform** built on graph
technology. It is **not limited to any predefined use case** and supports the creation of
**fully custom applications** — including web applications, graph-database-backed backends,
data-centric systems, middleware, and integration components.

At its core, Structr uses a **graph database** to store both application components (schema,
UI definitions, templates, scripts, workflows) and application/user data. This provides schema
flexibility, fast graph traversals, and expressive data modeling without join-heavy queries.

Structr supports representing its database schema as **JSON Schema**, including **import/export**
(see https://json-schema.org).

Structr includes an internal **virtual filesystem** for managing files and binary content with
custom metadata. Binary data can be stored externally on file storage providers (e.g., S3).

---

## What Structr is not

To set clear expectations, Structr is **not**:

- A prebuilt business application (e.g., CMS, CRM, ERP)
- A fixed-purpose framework with predefined domain logic
- A website builder with limited customization options
- A collection of templates tied to a specific industry
- A replacement for general-purpose programming languages

Instead, Structr is a **generic application platform** providing the building blocks,
abstractions, and runtime needed to **design, build, and operate your own applications**
within a controlled and extensible environment.

---

## Frontends and Development Model

Structr separates **application development** from **application usage** through two different
web-based user interfaces.

### Structr Developer / Admin UI (IDE)

Structr provides a web-based **Developer and Administration UI** that acts as an integrated
development environment (IDE). It exposes the platform’s capabilities through structured tools
for modeling, configuration, extension, and operation of applications defined in the graph.

The Admin UI is used to modify the **application graph**: the part of the database that defines
the application.

As a Structr developer, you typically **do not create or edit a raw source code tree** for your
app. Instead, development happens by editing high-level artifacts stored in the graph, such as:

- schemas, types, properties and relationships
- isolated script expressions and methods
- HTML templates and UI definitions
- virtual files with custom metadata
- configuration and operational settings

Structr supports **polyglot scripting via GraalVM** — i.e., you can use any language supported by
GraalVM for scripts and methods.

### End-User Application UI (Built with Structr)

Using the tools of the Structr IDE, developers create an end-user facing web UI that is served
directly by Structr. Pages, templates, bindings, and workflows are defined in the graph and can
be evolved at runtime.

---

## Developer & Admin Tools

Structr provides a comprehensive, web-based Developer and Administration UI that exposes
the platform’s core capabilities through structured tools. These tools are used to design,
configure, extend, and operate applications defined in the graph database.

### Platform & Operations
- Dashboard with system information and runtime status
- Server log and event log viewer
- Thread and job queue inspection
- Deployment and environment information
- UI configuration and settings

### Schema & Data Modeling
- Graph-based schema editor
- Type, property, and relationship management
- Function properties with read/write scripting
- Runtime schema evolution
- Virtual types management

### Data Management
- Graph-aware data browser and editor
- Spreadsheet-style CRUD editing
- Relationship linking and navigation
- Bulk data import (e.g. CSV)

### Page & UI Development
- Page and template editor
- Drag-and-drop UI components
- Dynamic repeaters bound to graph queries
- Templating expressions and localization
- Live preview of end-user pages

### Application Logic & Code
- Script editor for user-defined functions and methods
- Polyglot scripting via GraalVM
- Runtime execution and result inspection
- OpenAPI exposure configuration for types and methods

### Files & Assets
- Virtual filesystem browser
- Folder and file management
- Custom metadata for files and folders
- Binary files (images, videos, arbitrary content) with optional external storage (e.g. S3)
- Dynamic virtual files generated in real time via scripts
- File access via HTTP, FTP, and SSH/SCP (depending on deployment/configuration)
- Integration with background jobs (e.g. CSV imports)

### Graph Exploration
- Visual graph browser
- Cypher query execution
- Interactive node and relationship inspection

### Workflows & Automation
- Flow editor for orchestration logic
- Node-based workflow modeling
- Execution and result inspection

### Security & Access Control
- User and group management
- Role-based and resource-based permissions
- REST API access control
- CORS configuration

### Localization & Communication
- Localization key and translation management
- Mail template management

---

## AI, LLMs, and Low-Code as Guardrails

Structr’s combination of **no-code, low-code, and pro-code tools** makes it a strong foundation
for **AI- and LLM-assisted application development**.

Instead of allowing AI systems to generate unconstrained source code, Structr limits interaction
to **explicit schemas, structured tools, isolated scripts, and well-defined APIs**.

This means:
- AI operates within well-defined boundaries
- Changes are applied through structured models and tools
- Errors and unintended side effects are reduced

In this sense, **low-code provides guardrails for AI**. Structr allows AI systems to assist in
designing, extending, and operating applications while keeping full control over structure,
behavior, and security.

Structr can be used with AI to create apps and interact with the Structr application and the
data it manages through **MCP (Model Context Protocol)**.

---

## Architecture

- Java-based backend with a web frontend (admin IDE and end-user UI)
- Runtime: **GraalVM JDK 25**
- Graph database: **Neo4j 4.4+** (including 5.x and 2025.x)
- Monolithic core with modular/extensible design
- REST/JSON APIs for integration
- Virtual filesystem with custom metadata and optional external binary storage (S3)
- Optional containerized deployments (Docker/Podman), Kubernetes orchestration

Structr instances can be operated as small services that interact via REST APIs or through
event-driven / pub-sub protocols.

---

## Integration & Interoperability

- REST / JSON APIs
- JSON Schema import/export for schema representation
- Pub/sub and messaging: AMQP (RabbitMQ), Kafka, Pulsar, XMPP
- Email: SMTP (outbound), IMAP
- Identity & SSO: OAuth providers, LDAP-compatible services
- File access: HTTP, FTP, SSH / SCP
- Storage: S3-compatible repositories
- AI integration: MCP

---

## Licensing

Structr is **dual-licensed**:

- **Open Source**: GNU AGPL v3 or later and GNU GPL v3 or later
- **Commercial / Individual License**: available from Structr GmbH

See [LICENSE.md](LICENSE.md) and [COPYING.txt](COPYING.txt) for details.

---

## Security

Security policy and supported versions are documented in [SECURITY.md](SECURITY.md).

---

## Contributing

Questions and general discussions should go to the forum first:

- Questions and discussions: https://structr.org/forum
- GitHub issues: confirmed bug reports only

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

---

## Support

### Community
- Documentation
- Community forum
- Public issue tracking

### Commercial (Structr GmbH)
- Professional support and maintenance
- Custom development
- Architecture and integration consulting
- Hosting and managed services on Germany- or EU-based infrastructure

---

## Community & Documentation

- **Community site:** https://structr.org
- **Documentation:** https://structr.org/docs
- **Forum:** https://structr.org/forum

Contributions, feedback, and discussions are welcome.

---

## Data Protection & GDPR

Structr supports GDPR-compliant deployments:
- Fully self-hosted operation
- No mandatory external cloud services
- Standard hosting on Germany- or EU-based providers available

---

## Metadata

- **publiccode.yml:** [publiccode.yml](publiccode.yml)

---

Created with ❤️ by Structr GmbH and the Structr community.