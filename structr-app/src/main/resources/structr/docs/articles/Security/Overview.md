
Structr provides a security system that controls who can access your application and what they can do. This chapter covers authentication (verifying identity), authorization (granting permissions), and the tools to manage both.

## Core Concepts

Structr's security model is built on four pillars:

| Concept | Question it answers | Key mechanism |
|---------|---------------------|---------------|
| **Users & Groups** | Who are the actors? | User accounts, group membership, inheritance |
| **Authentication** | How do we verify identity? | Sessions, JWT, OAuth, two-factor |
| **Permissions** | What can each actor do? | Ownership, grants, visibility flags |
| **Access Control** | Which endpoints are accessible? | Resource Access Permissions |

These concepts work together: a request arrives, Structr authenticates the user (or treats them as anonymous), then checks permissions for the requested operation.

## Authentication

When a request reaches Structr, the authentication system determines the user context. Structr checks for a session cookie first, then for a JWT token in the Authorization header, then for X-User and X-Password headers. If none of these are present, Structr treats the request as anonymous.

Structr supports multiple authentication methods that you can combine based on your needs:

| Scenario | Recommended method |
|----------|--------------------|
| Web application with login form | Sessions |
| Single-page application (SPA) | JWT |
| Mobile app | JWT |
| Login via external provider (Google, Azure, GitHub) | OAuth |
| Your server calling Structr API | JWT or authentication headers |
| External system with its own identity provider | JWKS validation |
| High-security requirements | Any method combined with two-factor authentication |

The distinction between the last two server scenarios: when your own backend calls Structr, you control the credentials and can use JWT tokens that Structr issues or simple authentication headers. When an external system (like an Azure service principal) calls Structr with tokens from its own identity provider, Structr validates those tokens against the provider's JWKS endpoint.

## Permission Resolution

Once Structr knows who is making the request, it evaluates permissions for every operation the user attempts. Structr checks permissions in a specific order and stops at the first match:

1. Admin users bypass all permission checks
2. Visibility flags grant read access to public or authenticated users
3. Ownership grants full access to the object creator
4. Direct grants check SECURITY relationships to the user or their groups
5. Schema permissions check type-level grants for groups
6. Graph resolution follows permission propagation paths through relationships

For details on each level, see the User Management article.

## Getting Started

### Basic Web Application

A basic security setup for a typical web application involves creating users and groups in the Security area of the Admin UI, creating a Resource Access Permission with signature `_login` that allows POST for public users, implementing a login form that posts to `/structr/rest/login`, and configuring permissions on your data types.

### Adding OAuth

To add OAuth login, register your application with the OAuth provider, configure the provider settings in `structr.conf`, add login links pointing to `/oauth/<provider>/login`, and optionally implement `onOAuthLogin` to customize user creation.

### Adding Two-Factor Authentication

To add two-factor authentication, set `security.twofactorauthentication.level` to 1 (optional) or 2 (required), create a two-factor code entry page, and update your login flow to handle the 202 response.

### Securing an API

To secure a REST API for external consumers, create Resource Access Permissions for each endpoint you want to expose, configure JWT settings in `structr.conf`, implement token request and refresh logic in your API clients, and optionally configure CORS if clients run in browsers.

## Related Topics

- REST Interface / Authentication - Resource Access Permissions and CORS configuration
- SSL Configuration - Installing SSL certificates for HTTPS
- Configuration Interface - Security-related settings in structr.conf
- Admin UI / Security - Managing users and groups through the graphical interface
