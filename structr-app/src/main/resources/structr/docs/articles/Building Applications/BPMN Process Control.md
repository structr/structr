# BPMN Process Control

BPMN Process Control connects page elements to running business processes. It is an action of the Event Action Mapping system: when a user clicks a button or submits a form, Structr can start a new process instance, complete or claim a user task, send a signal, or perform administrative operations such as reassigning a task or suspending a process. All of this is configured declaratively on the element, without writing any code.

This chapter explains how the Control process action works. For a hands-on walkthrough that builds a complete process-based app, see the tutorial "Building A BPMN Process Based App".

## Basics

The action is configured on an element's **Events** tab by selecting the action `Control process`. Configuration is operation-first: choose the **Operation**, and the action shows exactly the fields that operation needs.

- **Operation**: what to do (start an instance, complete a task, ...). See the table below.
- **Process**: the process the action operates on. Required for every operation.
- **Process step**: the user task (or catch event) the action targets. Shown only for task-level operations and signals; whole-process operations do not need it.
- **Data type**: the subject type to instantiate. Shown only for `Complete a task and create the subject`, and filled automatically from the Subject type declared on the process.
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

- The **process declares** what it works on: the **Subject type** on the Process tab of the BPMN editor names the schema type. An instance has exactly one subject, so the type is a property of the process, not of a single task. Each user task can narrow which fields of the subject it shows and writes through its **Form view** and **Writable view**.
- The **page consumes** the declaration: when the operation `Complete a task and create the subject` is selected and the step is chosen, the action's Data type is filled from the process's Subject type automatically.

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

## Automatic steps

Not every step of a process needs a person. The engine executes gateways, script tasks and service tasks on its own while it advances the token, and pauses only at user tasks, message catch events and timers.

**Gateways** follow the sequence flow whose condition evaluates to true. A condition written in BPMN style as `${...}` is evaluated as JavaScript, so the comparison operators of other BPMN tools work unchanged; a condition written as a Structr expression is evaluated as StructrScript. The `default` attribute of the gateway names the flow to take when no condition matches, and a flow without a condition serves as fallback when there is no default. Set the attribute in the BPMN attributes editor of the gateway.

**Script tasks** run their script body. **Service tasks** run a script body as well when they have one, for example an expression imported from Camunda, and are passed through when they have none. The script format attribute selects the language. Scripts imported from Camunda or Flowable are transpiled to Structr JavaScript on the fly. A failing automatic task is logged and does not stop the instance.

Inside these scripts, the keyword `process` gives access to the process variables: the properties of the subject merged with the parameter values stored on the instance, where a parameter value wins over a subject property of the same name. Assigning `$.process.name = value` stores a new parameter value on the instance, routed like a submitted form parameter, so a script task can prepare data that a later gateway condition or user task reads.

## Timers

Timer start events, intermediate timer events and timer boundary events are supported. A timer definition of type `timeDuration` takes an ISO 8601 duration such as `PT30M`, `P1D` or `P2DT3H` and fires that long after the token arrives. A definition of type `timeDate` takes an ISO 8601 instant such as `2026-04-25T14:30:00Z` and fires at that time; an instant without zone is interpreted in the server's time zone. Cycles of type `timeCycle` are not supported yet and are ignored with a warning. A boundary timer with `cancelActivity` set to true interrupts the task it is attached to when it fires.

Timers are stored as nodes, so pending timers survive a restart, and timers that elapsed while the server was down fire on startup. The `ProcessTimerService` polls for due timers every five seconds and fires each one in its own transaction. The service is part of the default service list. If you have customized `configured.services` in `structr.conf`, make sure the service is still listed, otherwise timers are created but never fire.

## Event handlers

A handler is a schema method that the engine runs when a task or a process instance passes through a lifecycle event. You create handlers in the BPMN editor: the Element tab of a user task and the Process tab of the process each have an "Add handler" button that asks for the event and the phase and creates a method named after the step, the phase and the event, for example `reviewRequest_afterCompleted`. Open the method in the Code area to write its body. Handler names are scoped per process and version, so two processes can both have an `onCreated` handler without conflict.

| Level | Events |
|---|---|
| Task | `created`, `assigned`, `claimed`, `available`, `declined`, `completed`, `cancelled` |
| Process | `created`, `started`, `subjectAttached`, `completed`, `terminated`, `suspended`, `resumed` |

The **phase** decides when the method runs. `on` runs inside the engine's transaction before the transition is committed; an exception thrown by the method rolls the transition back, which turns the handler into a validation or veto step. `after` is the default and runs after the commit, so it only fires when the transition actually persisted. Use it for side effects such as notifications, audit entries or derived data. There is one handler per event and phase.

Handlers receive named arguments, which you read via `$.args` as in any other schema method. A task handler gets `task`, `processInstance`, `subject` (when one is attached) and `eventName`, plus all parameter values of the instance by name. On the `completed` event the just-submitted form parameters are included before they are persisted, so an `on` handler can inspect or reject them. A process handler gets `processInstance`, `subject`, `eventName` and the parameter values. The task or instance is also available as `this`.

## Versions

Every process carries a version. Importing a BPMN file whose process id already exists does not overwrite the process but creates the next version, and copies the handler methods of the previous version onto it so that existing logic keeps working. Instances always execute the version they were started on, which lets you change a model while older instances complete on the old one. The process pickers in the Events and Process tabs show the version next to the name, for example "Request Process (v2)", so a page element is always bound to a specific version. Running instances keep their definition even when a later application deployment removes it; the definition disappears once those instances are finished.

## Notification links and sessionless access

Participants who do not log in to your application, for example an external approver, can act on a task through a signed link. This requires the **security level** of the process definition to be set to `low` in its properties; the default `high` only allows access through the pages of the application while logged in.

The link is built in a handler, typically on the `assigned` or `created` event of the task:

```javascript
{
    let instance = $.args.processInstance;
    let task     = $.args.task;
    let token    = $.processToken(instance.id, task.id, 'review');
    let url      = $.processInstanceUrl(instance, token);

    $.processNotify('email', $.args.subject.approverEmail, 'Please review', 'Open the request: ' + url);
}
```

`processToken(processInstanceId, taskId, action [, expiryMinutes])` creates a signed JSON Web Token that carries the instance, the task and the allowed action. It expires after 48 hours unless you pass a different expiry, and requires `security.jwt.secret` in `structr.conf` to have at least 32 characters. `processInstanceUrl(instance [, token])` builds the absolute URL of the instance page and appends the token as `?token=`. The base URL comes from the Site the instance page is assigned to, then from `application.baseurl.override`, then from the current request, so the function also works in handlers that run without a request. `processNotify(channel, recipient, subject, message)` sends the message. The channel `email` uses the SMTP configuration of the instance, the channel `log` writes to the server log and is useful while testing.

The instance page is responsible for honouring the token. It reads the `token` request parameter, validates it with `validateProcessToken(token)` inside `doPrivileged`, and checks that the returned `processInstanceId` matches the instance in the URL and that the task is still open before it renders the form and completes the task on the visitor's behalf. The function returns the claims of a valid token and `null` for an invalid, expired or tampered one.

## Process-bound components and page skeletons

Two features shorten the way from model to page. The "Create page skeleton" button on the Process tab of the BPMN editor generates the instance page with one section per human step, each already carrying the matching visibility rule, and binds it as the instance page. The process-bound components of the default widget set render the subject of the instance from the subject type of the process, so a form for a user task needs no manual field setup. Both are described in the Processes chapter of the Admin User Interface section and in the Widgets & Components chapter.

## Related topics

- **Event Action Mapping**: events, actions, parameter mappings, notifications and follow-up actions in general.
- **Processes** (Admin User Interface): the BPMN editor, process settings, page skeletons and the monitoring views for instances and tasks.
- **Building A BPMN Process Based App** (Tutorials): a step-by-step walkthrough using the concepts of this chapter.
