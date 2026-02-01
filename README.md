# Structr
[![Structr CI Pipeline](https://github.com/structr/structr/actions/workflows/main.yaml/badge.svg)](https://github.com/structr/structr/actions/workflows/main.yaml)
[![Hits-of-Code](https://hitsofcode.com/github/structr/structr?branch=main)](https://hitsofcode.com/github/structr/structr/view?branch=main)
![Docker Pulls](https://img.shields.io/docker/pulls/structr/structr)
[![publiccode.yml](https://img.shields.io/badge/publiccode.yml-present-brightgreen)](publiccode.yml)

![License: AGPL v3 / GPL v3](https://img.shields.io/badge/license-AGPLv3%20%2F%20GPLv3-blue.svg)
![Java](https://img.shields.io/badge/Java-GraalVM%20JDK%2025-orange)
![Graph Database](https://img.shields.io/badge/Database-Graph%20(Neo4j)-brightgreen)
![Low Code](https://img.shields.io/badge/Low--Code-Platform-blueviolet)
![REST API](https://img.shields.io/badge/API-REST%20%2F%20JSON-informational)
![Event Driven](https://img.shields.io/badge/Integration-Event--Driven-yellow)
![AI Ready](https://img.shields.io/badge/AI-MCP%20Enabled-ff69b4)
![GDPR](https://img.shields.io/badge/GDPR-Compliant-success)
![EU Based](https://img.shields.io/badge/Hosting-EU%20%2F%20Germany-blue)

**Structr** is an open source low-code development and runtime platform built on graph technology.  
It combines a Java-based backend with a web-based frontend to enable the development and operation of data-driven web applications with transparent data models, extensible business logic, and self-hosted deployment.

**TL;DR**
- Graph-native low-code platform
- Java backend, web-based frontend
- Graph database for application components *and* user data
- REST + event-driven integration
- Self-hosted, GDPR-friendly, EU-based support available

---

## Why Structr (Key Benefits)

- **Graph-native application model**  
  All application components and data are stored in a graph database, enabling schema flexibility and efficient handling of highly connected data.

- **High performance for complex domains**  
  Direct relationship traversal, index-free adjacency, and avoidance of join-heavy queries enable fast applications even with complex data models.

- **Low-code and pro-code combined**  
  Rapid development using modeling and configuration, with full extensibility via scripting and custom modules.

- **Middleware and integration hub**  
  Designed to integrate cleanly into existing system landscapes via APIs and messaging.

- **Self-hosted and data-sovereign**  
  Suitable for on-premises and private cloud deployments, including EU- and Germany-based infrastructure.

---

## Architecture

Structr follows a **monolithic core architecture** with a **modular and extensible design**.

Key characteristics:

- Java-based backend runtime with web-based frontend
- Central graph database (Neo4j) used for:
    - High-level abstraction of all application components
    - Default storage for application and user data
- Schema-flexible data modeling
- Efficient graph traversals instead of expensive table joins
- Internal virtual filesystem with metadata stored in the graph
- Optional external storage for binary data (e.g. S3-compatible object storage)

Structr instances can be deployed as standalone services or as part of distributed system landscapes.

---

## Features

- Java-based backend and web-based frontend
- Graph database integration (Neo4j)
- Low-code application modeling
- Pro-code extensibility (scripting, custom modules)
- REST and JSON APIs
- Event-driven and pub/sub integration
- Virtual filesystem with custom metadata
- External binary storage support (S3-compatible)
- Authentication and identity integration:
    - OAuth 2.0
    - OpenID Connect
    - LDAP
- AI-assisted application development via Model Context Protocol (MCP)
- Container-ready (Docker, Podman)
- Kubernetes-compatible
- Enterprise-grade security features

---

## Integration & Interoperability

Structr integrates with other systems using open standards and widely adopted protocols:

- REST, JSON
- OAuth 2.0, OpenID Connect
- LDAP
- SMTP, IMAP
- AMQP (e.g. RabbitMQ)
- Apache Kafka
- Apache Pulsar
- XMPP
- S3 API
- Model Context Protocol (MCP)

This enables both synchronous API-based integration and asynchronous, event-driven architectures.

---

## AI & Automation

Structr exposes APIs that allow **AI-based systems** to assist in application development and interact with running applications and managed data.

Using the **Model Context Protocol (MCP)**, AI tools can:

- Inspect schemas and application models
- Create or modify application components
- Interact with data and workflows
- Manage files and metadata

All AI-driven interactions follow the same authentication and authorization rules as human users.

---

## Licensing

Structr is **dual-licensed**:

### Open Source
- GNU AGPL v3 or later
- GNU GPL v3 or later

### Commercial / Individual License
- Available from Structr GmbH
- Includes alternative licensing models, professional support, and custom agreements

See the `LICENSE` file for details.

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

Structr is designed to support **GDPR-compliant deployments**:

- Fully self-hosted operation
- No mandatory external cloud services
- Support for Germany- and EU-based infrastructure providers
- Clear separation of metadata, application data, and binary content

---

## Copyright

© Structr GmbH  
Structr is developed and maintained by Structr GmbH and the open source community.