# BPMN Process Control

BPMN Process Control connects page elements to running business processes. It is an action of the Event Action Mapping system: when a user clicks a button or submits a form, Structr can start a new process instance, complete or claim a user task, send a signal, or perform administrative operations such as reassigning a task or suspending a process. All of this is configured declaratively on the element, without writing any code.

This chapter explains how the Control process action works. For a hands-on walkthrough that builds a complete process-based app, see the tutorial "Building A BPMN Process Based App".

## Basics

The action is configured on an element's **Events** tab by selecting the action `Control process`. Configuration is operation-first: choose the **Operation**, and the action shows exactly the fields that operation needs.

- **Operation**: what to do (start an instance, complete a task, ...). See the table below.
- **Process**: the process the action operates on. Required for every operation.
- **Process step**: the user task (or catch event) the action targets. Shown only for task-level operations and signals; whole-process operations do not need it.
- **Data type**: the subject type to instantiate. Shown only for `Complete a task and create the subject`, and filled automatically from the step's declared Subject type.
- **ID expression**: a script expression that identifies the concrete process instance (or task instance) at runtime, typically `${current.id}`.

Like every Event Action Mapping, the action supports parameter mappings, notifications and follow-up actions; the process-specific behaviour of these is described below.

## Process operations

| Operation | Group | Who may call it | Effect |
|---|---|---|---|
| Start a new process instance | Start | any user with read access on the process definition | creates a `ProcessInstance`, sets the caller as initiator, places the token at the start event and advances to the first wait state |
| Claim a task | Tasks | a candidate assignee (directly or via group) | reserves an available task for the caller |
| Complete a task | Tasks | the task's participant (the task UI is the gate) | completes the task, routes submitted parameters, advances the process |
| Complete a task and create the subject | Tasks | the task's participant | creates the subject object, transfers submitted parameters into it, attaches it to the instance, completes the task |
| Release a task back to the pool | Tasks | the current assignee | returns a reserved task to `available` |
| Decline a task (vote, reversible) | Tasks | a candidate assignee | records a decline vote; no state change, reversible by claiming |
| Delegate a task to someone else | Tasks | the current assignee, or a candidate while the task is available | hands the task to another user or group |
| Cancel a task (admin) | Tasks (admin) | users with access control on the task | cancels the task without advancing the process |
| Make a task available again (admin) | Tasks (admin) | users with access control on the task | clears the assignee and returns the task to the candidate pool |
| Reassign a task to a chosen user (admin) | Tasks (admin) | users with access control on the task | sets a new assignee directly, overriding candidate declarations |
| Send a signal to a running process | Signals | participants of the instance | resumes a waiting intermediate catch event |
| Suspend a running process (admin) | Lifecycle (admin) | administrative users | pauses token advancement; tokens stay in place |
| Resume a suspended process (admin) | Lifecycle (admin) | administrative users | continues a suspended instance |
| Terminate a process (admin) | Lifecycle (admin) | administrative users | ends the instance; all waiting tokens are consumed without advancement |

Participant operations follow the standard human-task vocabulary: users claim, complete, release, decline and delegate their own work. Administrative operations require access control permission and exist for routing and lifecycle intervention.

## Starting a process

The start operation needs only the **Process**. Two ways to select it:

- **Static**: pick the process in the Process dropdown. This is the normal case for a dedicated start button.
- **Dynamic**: leave the Process empty and set a **Dynamic process UUID** expression instead, for example `${current.id}` on a process catalog page where each row is bound to a different process. The expression is resolved at page render time and takes precedence over the static selection. Dynamic selection is honoured for the start operation only.

The start operation's response contains a ready-made **URL of the new instance's page**: the page bound as the process's **Instance page** (a setting on the Process tab of the BPMN editor), or, when no page is bound, a path built from the slugified process name, for example `/request-process/<instance-uuid>`. A follow-up action `Navigate to a new page` with Success URL `{result.url}` therefore drops the user directly onto the freshly created instance's page.

Parameters declared on a start action are stored as initial process parameters of the new instance, where process listeners and later steps can read them.

## Targeting tasks: the ID expression

Task-level operations act on one concrete `TaskInstance`. The action finds it through the **ID expression**:

- If the expression resolves to a **TaskInstance** UUID, that task is used directly.
- If it resolves to a **ProcessInstance** UUID (the typical `${current.id}` on an instance page), the engine locates the caller's active task for the configured **Process step** on that instance.

The second form is the common pattern: the instance page receives the process instance as its `current` object via the UUID path segment of the URL, and every task action on the page targets its step with `${current.id}`. The Process step must be set for this resolution to work; an action without it fails with an explanatory error.

## The subject contract

A process instance operates on at most one domain object: the **subject** (a `LeaveRequest`, an `Invoice`, a `Request`). The connection between process and page is a declared contract:

- The **user task declares** what it works on: its **Subject type** field in the BPMN editor names the schema type.
- The **page consumes** the declaration: when the operation `Complete a task and create the subject` is selected and the step is chosen, the action's Data type is filled from the step's Subject type automatically.

`Complete a task and create the subject` is the standard entry point for the first user task of a process. In one step it creates the subject object, transfers the submitted parameters into it, attaches it to the process instance, and completes the task. The subject is created by the engine rather than by the submitting user, so the submitter does not become its owner; access flows through the engine's participant grants on the instance instead.

### How parameters are routed

Form values reach the operation as named parameters, declared in the action's **Parameter Mapping** section (parameter type `User Input`, with the input element linked to the parameter). On completion, the engine routes each parameter by name:

- A parameter whose name **matches a property of the subject's type** is written to the subject: a parameter `title` becomes the subject's `title`.
- Any other parameter is stored as a **process parameter value** on the instance, readable by listeners, gateway conditions and later steps.

The parameter names are the contract. The reserved names `id` and `type` are never written to the subject. Subsequent tasks use the plain `Complete a task` operation, since the subject already exists; the same routing rule then updates it.

## Showing and hiding elements by process state

Pages that host process actions usually adapt to the state of the process: the form for a task should show only while that task is open, a confirmation only after completion. Process Visibility Rules (internal type: `VisibilityMapping`) declare this on the element's **Process** tab, one rule per element, no code:

| Visible when in state | Meaning |
|---|---|
| A task is available to claim | the bound step's task is `available` |
| A task is reserved by me | the current user holds the bound step's task |
| A task is reserved by someone else | another user holds the task |
| A task has been completed | the bound step's task is `completed` |
| A task has been cancelled | the bound step's task is `cancelled` |
| Process has completed | the instance reached an end event |
| Process has been terminated | the instance was terminated |
| Process has failed | the instance is in a failure state |
| Process is awaiting someone else's action | a task is open, but not for the current user |
| No process instance exists for me yet | the current user has no active instance of the process |
| I already have a running instance | the current user has an active instance of the process |

Task-scoped and most process-scoped states evaluate against the page's `current` object, so they belong on the instance page. The two states at the bottom query the database for the current user's instances and work on any page, which makes them the right choice for start buttons and catalog pages. Multiple rules on one element are OR-combined, and a rule hides the element's entire subtree, so it belongs on exactly the element it should control.

## Related topics

- **Event Action Mapping**: events, actions, parameter mappings, notifications and follow-up actions in general.
- **Building A BPMN Process Based App** (Tutorials): a step-by-step walkthrough using the concepts of this chapter.
