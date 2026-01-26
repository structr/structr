
Event Action Mapping is Structr's approach to handling user interactions. It connects DOM events directly to backend operations without writing JavaScript code. When a user clicks a button, submits a form, or changes an input field, Structr can respond by creating, updating, or deleting data, calling methods, or navigating to another page.

## Why Event Action Mapping

In traditional web development, handling user interactions requires multiple layers: JavaScript event listeners on the client, API endpoints on the server, and code to connect them. You write fetch calls, handle responses, update the DOM, and manage error states – all manually.

Event Action Mapping eliminates this boilerplate. You configure what should happen when an event fires, and Structr handles the communication between client and server. This keeps the simplicity of server-side rendering while adding the interactivity users expect from modern web applications.

Because Event Action Mapping is declarative, you can see at a glance what each element does. There is no hidden logic in JavaScript files – the behavior is configured directly on the element in the Pages area. This is part of Structr's low-code approach: you build interactive applications by configuring behavior rather than writing code.

## How it Works

An Event Action Mapping consists of four parts: an event, an action, parameters, and optionally one or more follow-up actions.

The **event** is a DOM event like `click`, `submit`, or `change` that triggers the mapping. You configure which event to listen for on each element.

The **action** defines what happens when the event fires. This can be a data operation like create or update, a method call, or a built-in action like login or logout.

The **parameters** define which data is sent with the action. You can map input fields, constant values, or evaluated expressions to parameter names.

The **follow-up action** defines what happens after the action completes. This can be a navigation to another page, a partial reload to update part of the page, or another action.

### Debouncing

Event Action Mapping automatically debounces requests. When multiple events fire in quick succession, Structr waits until the events stop before sending the request. This prevents duplicate submissions when a user accidentally double-clicks a button or types quickly in an input field with a `change` or `input` event.

### The Frontend Library

To enable Event Action Mapping, your page must include the Structr frontend library:

`<script type="module" defer src="/structr/js/frontend/frontend.js"></script>`

This script listens for configured events and sends the corresponding requests to the server. The page templates included with Structr already include this library, so you only need to add it manually if you create your own page template from scratch.

## Events

Events are DOM events that trigger an action. You configure which event to listen for on each element.

### Available Events

The input field provides suggestions for commonly used events like `click`, `submit`, `change`, `input`, `key down`, and `mouse over`. You are not limited to these suggestions – the event field accepts any DOM event name, giving you full flexibility to react to any event the browser supports.

### Choosing the Right Event

The choice of event depends on the element and the desired behavior. For buttons, `click` is the typical choice. For forms, you usually listen for `submit` on the form element rather than `click` on the submit button. For checkboxes and radio buttons, use `change` instead of `click` – the value only updates after the click event completes.

### Forms Without Form Elements

Event Action Mapping does not require a `<form>` element. You can wire up individual input fields to save their values independently, for example by listening for `change` on each field and triggering an update action. This allows for auto-save interfaces where each field saves immediately when the user makes a change.

## Actions

Actions define what happens when an event fires. Each action type has its own configuration options. Most actions require parameters to specify which data to send to the server.

### Create

Creates a new object in the database. The primary configuration is the type of object to create, which you enter in the "Enter or select type of data object" field.

You can also enter `AbstractNode` as the type and pass a parameter named `type` to determine the actual type at runtime. This is useful when the type depends on request data.

### Update

Updates an existing object in the database. You specify the type of the object and enter a template expression in the "UUID of data object to update" field that resolves to the object's ID, for example `${current.id}` or `${project.id}`.

Note that this is not an auto-script field, so you need to include the `${...}` wrapper.

### Delete

Deletes an object from the database. You enter a template expression in the "UUID of data object to delete" field that resolves to the object's ID, for example `${current.id}` or `${project.id}`.

### Sign In

Authenticates a user. Requires either username and password, or email and password.

### Sign Out

Ends the current user session. This action requires no parameters.

### Sign Up

Creates a new user account.

### Reset Password

Initiates the password reset process for a user.

### Call Method

Calls a method defined in your data model. You specify the UUID of the object on which to execute the method and the method name. You can pass arbitrary parameters to the method, which become available under `$.arguments` in the method body.

### Execute Flow

Executes a Structr Flow.

## Parameters

Parameters define which data is sent with an action. To add a parameter, click the plus button next to the "Parameter Mapping" heading. Each parameter has a name and a type that determines where the value comes from.

### Parameter Types

**User Input**: Links to an input field on the page. You can drag and drop an input element directly into the parameter configuration.

**Constant Value**: A fixed value that is always sent with the action.

**Evaluate Expression**: A template expression that is evaluated when the action fires.

**Result of Method Call**: The return value of a method.

**Result of Flow**: The return value of a Structr Flow.

**Request Parameter for Page**: A request parameter used for pagination.

**Request Parameter for Page Size**: A request parameter that specifies the page size for pagination.

## Notifications

Notifications provide visual feedback to the user about whether an action succeeded or failed. You configure success notifications and failure notifications separately, allowing different feedback for each outcome.

### Success Notifications

Success notifications are shown when the action completes successfully.

### Failure Notifications

Failure notifications are shown when the action fails. They have access to the response code and can display the error message returned by the server.

### Notification Types

**None**: No notification is shown.

**System Alert**: Displays a browser alert dialog.

**Inline Text Message**: Displays a text message on the page. You can configure the display duration in milliseconds.

**Custom Dialog Element Defined by CSS**: Displays a custom element selected by CSS selector. This option can also trigger a partial refresh.

**Custom Dialog Element Defined by Linked Element**: Displays a custom element that you link directly in the configuration. This option can also trigger a partial refresh.

**Raise a Custom Event**: Raises a custom DOM event that you can handle with JavaScript.

## Follow-up Actions

Follow-up actions define what happens after an action completes. You configure success follow-up actions and failure follow-up actions separately, allowing different behavior for each outcome.

### Success Follow-up Actions

Success follow-up actions are executed when the action completes successfully.

### Failure Follow-up Actions

Failure follow-up actions are executed when the action fails.

### Follow-up Action Types

**None**: No follow-up action.

**Reload the Current Page**: Reloads the entire page.

**Refresh Page Sections Based on CSS Selectors**: Reloads specific parts of the page selected by CSS selectors. This is useful for updating a list after creating or deleting an item without reloading the entire page.

**Refresh Page Sections Based on Linked Elements**: Reloads specific elements that you link directly in the configuration.

**Navigate to a New Page**: Navigates to another page. You can include the ID of a newly created object in the URL to navigate directly to its detail page.

**Raise a Custom Event**: Raises a custom DOM event that you can handle with JavaScript.

**Sign Out**: Ends the current user session.

## Validation

There are two approaches to validating user input: client-side validation before the request is sent, and server-side validation when the data is processed.

### Client-Side Validation

For client-side validation, you can use standard HTML5 validation attributes on your form fields:

- `required` – the field must have a value
- `pattern` – the value must match a regular expression
- `min` and `max` – for numeric ranges
- `minlength` and `maxlength` – for text length

The browser validates these constraints before the form is submitted. If validation fails, the browser shows an error message and the request is not sent.

#### Validation Events

HTML provides events that you can use to extend validation behavior:

- `invalid` – fires when a field fails validation
- `input` – fires when the value changes, useful for live validation
- `change` – fires when the field loses focus after the value changed

You can use these events with Event Action Mapping to trigger custom validation logic or display custom error messages.

#### Validation CSS Classes

The browser automatically applies CSS pseudo-classes to form fields based on their validation state:

- `:valid` – the field value meets all constraints
- `:invalid` – the field value fails validation
- `:required` – the field is required
- `:optional` – the field is not required

You can use these pseudo-classes to style fields differently based on their validation state, for example showing a red border on invalid fields or a green checkmark on valid ones.

For more complex validation logic, you need to implement custom JavaScript.

### Server-Side Validation

Server-side validation happens when the data reaches the backend. Structr validates the data against the constraints defined in your data model. If validation fails, the server returns an error response that you can display to the user using a failure notification.

#### Schema Constraints

The data model provides several validation options:

- **Not-null constraints**: Properties marked as not-null must have a value. Creating or updating an object without a required property fails with a validation error.

- **Uniqueness constraints**: Properties marked as unique must have a value that no other object of the same type has. This is useful for email addresses, usernames, or other identifiers.

- **Compound uniqueness**: Multiple properties can be marked for compound uniqueness, ensuring their combined values are unique across all objects of the type.

- **Format patterns**: String properties can have a format pattern that the value must match, defined as a regular expression.

- **Value ranges**: Numeric properties can have minimum and maximum values.

#### Lifecycle Methods

For complex validation that goes beyond schema constraints, you can implement validation logic in lifecycle methods. The `onCreate` and `onSave` methods are called before an object is created or modified, allowing you to validate the data and throw an error if it is invalid. For details on lifecycle methods, see the Business Logic chapter.

#### Handling Validation Errors

When server-side validation fails, Structr returns an error response with details about which constraints were violated. You can display this information to the user using a failure notification. This approach means the user only sees validation errors after the request completes, but it ensures that all constraints defined in your schema are enforced.

## Custom JavaScript Integration

Event Action Mapping covers the most common interaction patterns, but sometimes you need more control. The "Raise a Custom Event" option allows you to break out of the Event Action Mapping framework and integrate custom JavaScript logic.

When you configure a notification or follow-up action to raise a custom event, Structr dispatches a DOM event that you can listen for in your own JavaScript code. This lets you combine the simplicity of Event Action Mapping with custom logic when needed.

For example, you can use Event Action Mapping to handle form submission and data creation, then raise a custom event to trigger a complex animation, update a third-party component, or perform additional client-side processing.

This ensures that advanced users always have an escape hatch to take full control when the declarative approach is not sufficient.