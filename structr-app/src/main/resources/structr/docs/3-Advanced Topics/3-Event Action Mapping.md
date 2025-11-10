# Event Action Mapping

Event Action Mapping is a powerful feature in Structr that enables you to create interactive web applications without writing any custom JavaScript code.

It allows you to define what happens when users interact with the elements of your application by mapping frontend events (e.g. click, form submissions key press) to backend actions (like create, update, delete data).

## Overview

Event Action Mapping bridges the gap between frontend user interactions and backend data operations. Instead of writing custom JavaScript and AJAX calls, you configure mappings that define:

1. **Frontend Events**: What user action triggers the response (click, submit, keydown, etc.)
2. **Backend Actions**: What operation to perform (create, update, delete, method call)
3. **Success/Failure Handling**: What to do when the action succeeds or fails

## Core Concepts

### Frontend Events

Structr supports all standard HTML DOM events:

- **Mouse Events**: click, mouseover, mouseout, mouseenter, mouseleave
- **Keyboard Events**: keydown, keyup, keypress
- **Form Events**: submit, change, input, focusout
- **Document Events**: load, unload, copy, cut, paste
- **Custom Events**: User-defined events for complex workflows

### Backend Actions

Available backend actions include:

- **CRUD Operations**: create, update, delete
- **Method Calls**: Execute custom schema methods
- **Flow Execution**: Run visual Flow diagrams
- **Navigation**: next-page, prev-page
- **Authentication**: sign-in, sign-out, sign-up, reset-password

### Success/Failure Behaviors

Define what happens after an action completes:

- **Page Actions**: full-page-reload, navigate-to-url
- **Partial Updates**: partial-refresh, partial-refresh-linked
- **Notifications**: system-alert, inline-text-message, custom-dialog
- **Events**: fire-event for chaining actions
