# Building A BPMN Process Based App

In this tutorial you will build a minimal but complete business process: the "Request Process". It consists of a single user task, wired to a regular Structr page via Event Action Mapping (EAM). By the end, you can start a process instance by clicking a button, fill out a form that completes the user task and creates the domain object the process works on, and control which parts of the page are visible depending on the state of the process.

Every step is verifiable in the Admin UI before you move on to the next one, so you always know your setup works.

## Prerequisites

- **Admin access** to a running Structr instance with the Process Engine available (the **Processes** area appears in the extended menu behind the ☰ icon in the main navigation).
- **Pages to wire against.** This tutorial creates two small pages from the **Simple Page** template as it goes.
- **The frontend library.** Event Action Mapping only works on pages that include Structr's frontend library:

```html
<script type="module" defer src="/structr/js/frontend/frontend.js"></script>
```

The minimal **Simple Page** template does *not* include this script, so adding it is part of building each page below: select the page's `head` element, insert a `script` element, and set its `type` attribute to `module` and its `src` attribute to `/structr/js/frontend/frontend.js` on the HTML tab. (Some richer page templates ship with the library already included; check the page source for `frontend.js` if you are unsure.) Without it, none of the wiring in this tutorial has any effect. See the Event Action Mapping chapter for details.

## Create the BPMN process

1. Open the **Processes** area (☰ menu) and switch to the **Process Definitions** tab.
2. Click **Create Process Definition**: the BPMN diagram editor opens.
3. Model the minimal structure: from the palette, place a **Start** event, a **User** task, and an **End** event on the canvas (click the palette entry, then click the target position). Connect them: select the start event and drag from the small **+** handle at its edge onto the task, then select the task and drag from its **+** handle onto the end event.

```
(Start) ──> [ submitRequest ] ──> (End)
```

4. Select the user task and set its **Name** to `submitRequest` in the side panel.
5. Switch the side panel to the **Process** tab and set **Process name** to `Request Process`. This is the name the process carries at runtime and in all process pickers.
6. **Save** the diagram.
7. Select the user task again (side panel back on the **Element** tab) and set **Assignee (humanPerformer)** to `${initiator}`. This assigns the task to whoever starts the process, which is exactly right for a self-service form: the person who starts the request fills it out. The assignee is stored immediately; note that it can only be set on a saved task, which is why this step comes after saving. (Tasks for *other* people use candidate groups instead; see "Extending the process" below. Alternatively, the Process tab's **Auto-assign tasks to the initiator** option achieves the same without declaring a performer.)
8. Close the editor.

**Verify:** the Process Definitions tab lists your definition. The list shows the *definition's* name (a generated one like "New process definition 12345"); the process name you set lives inside it, on the Process tab. To make the list read "Request Process" too, rename the definition via its edit (pencil) action.

## Start a process instance from a button

Now wire a button on your page to start the process.

1. In the **Pages** area, select (or create) a button element on your page.
2. Switch to the element's **Events** tab and configure:
   - **Event:** `Click`
   - **Action:** `Control process`
   - **Operation:** `Start a new process instance`
   - **Process:** `Request Process`

That is the complete wiring: clicking the button now creates a new process instance, places it at the start event, and advances it to the user task, where it waits.

**Verify:** view the page as a logged-in user and click the button. Then open **Processes → Process Instances** in the Admin UI: a new instance of "Request Process" appears, with status `running`. The **Task Instances** tab shows the `submitRequest` task with status `reserved` and the user who clicked as its assignee.

## Create the subject type and form

A process usually operates on a domain object: the *subject*. For the Request Process, the subject is a `Request`. The user task `submitRequest` is the step where that object gets created and filled.

1. **Create the type.** In the **Code** area (or Schema editor), create a new type `Request` with the properties your form should capture, for example `title` (String) and `description` (String).
2. **Declare the contract on the task.** Back in the BPMN editor, select the `submitRequest` task and set its **Subject type** field to `Request`. The process now declares *what* this task works on; the page you build next merely consumes that declaration.
3. **Create the instance page.** Create a page named `request-process`. The name matters: when a process instance is started, the response contains a ready-made URL of the form `/request-process/<instance-uuid>` (the slugified process name plus the new instance's UUID). A page with the matching name picks the instance up as its `current` object via the UUID path segment, which the form wiring below relies on.
4. **Build the form** on that page: a real `form` element with one input per `Request` property you want to capture, plus a submit button:

```html
<form id="request-form" method="post">
    <input type="text" name="title" placeholder="Title">
    <input type="text" name="description" placeholder="Description">
    <button type="submit">Submit request</button>
</form>
```

Use an actual `form` element (not just a `div`): the wiring in the next section listens for the form's `submit` event. The input **names should match the property names** of `Request`: that is how the engine knows where the values belong (see the callout below).

## Wire the form to the user task

First, send the user to the instance page after starting. Go back to the start button's **Events** tab and add the follow-up action:

- **Behaviour on success:** `Navigate to a new page`
- **Success URL:** `{result.url}`

`{result.url}` is filled from the start operation's response and points at `/request-process/<new-instance-uuid>`, so every click lands on the freshly created instance's page.

Now wire the form itself:

1. Select the `form` element, switch to its **Events** tab and configure:
   - **Event:** `submit`
   - **Action:** `Control process`
   - **Operation:** `Complete a task and create the subject`
   - **Process:** `Request Process`
   - **Process step:** `submitRequest`
   - **Data type:** filled automatically with `Request`, taken from the step's Subject type declaration
   - **ID expression:** `${current.id}` (the process instance from the page URL; the engine locates your active `submitRequest` task from it)
2. In the **Parameter Mapping** section, declare one parameter per form field: add a parameter named `title` of type **User Input** and drag the title input element into its dropzone; repeat for `description`. This binds each input's value to a named parameter of the completion request (the same mechanism as in the Event Action Mapping chapter's form examples).
3. Add a follow-up action: **Behaviour on success:** `Reload the current page`.

The operation `Complete a task and create the subject` is the ideal entry point for the first user task of a process: it creates the `Request` object, transfers the submitted form data into it, attaches it to the process instance as the subject, and completes the task in one step.

> **How parameters are routed.** When a task is completed, every submitted parameter whose **name matches a property of the subject's type** is written directly to the subject. All other parameters are passed on to the process engine and stored as process parameter values on the instance. The parameter names are the contract: a parameter named `title` becomes `request.title`; one named `internalNote` (no such property on `Request`) becomes engine-level process metadata instead.

**Verify:** click the start button, land on the instance page, fill in the form and submit. In the Admin UI: **Processes → Task Instances** shows `submitRequest` as `completed`, **Process Instances** shows the instance as `completed` (the token moved through the end event), and the Data area shows a new `Request` object carrying your form values as its subject data.

## Control form visibility by process state

Right now the form is always visible, even after the task is done. Process Visibility Rules (internal type: `VisibilityMapping`) bind an element's visibility to the state of a process, declaratively and without writing any show/hide logic.

1. In the **Pages** area, select the `form` element (`form#request-form` in the example above).
2. Switch to the **Process** tab and click **Add visibility rule**:
   - **Process:** `Request Process (v1)` (the picker shows the process with its definition version)
   - **Step (task):** `submitRequest`
   - **Visible when in state:** `A task is reserved by me`
3. The rule is stored as soon as all three fields are set. The form now renders only while the current user actually holds the open `submitRequest` task of this instance.

State evaluation uses the page's `current` object (the process instance from the URL), which the instance page already provides. Multiple rules on the same element are OR-combined. Put each rule on exactly the element it should control: a rule on a surrounding container hides everything inside it, including elements with rules of their own.

Two useful companions:

- Add a "thank you" section to the instance page, as a *sibling* of the form (for example a `div` next to it), with a rule **Visible when in state:** `Process has completed`. After submitting, the page reloads, the form disappears and the confirmation appears: the full state round-trip, with zero custom code.
- On the page hosting the start button, a rule with state `No process instance exists for me yet` hides the button once the user already has a running request (this particular state works even without a `current` process instance on the page).

**Verify:** open the instance page before submitting: the form is visible. Submit: the page reloads, the form is gone, the completed-state section shows.

## Extending the process

Every further step follows the same pattern you just used. To add a review step after submission:

1. **Extend the diagram:** add a second user task `reviewRequest` between `submitRequest` and the end event. Assign it to the person or group who reviews (for a group, use a candidate expression instead of the assignee; members then claim the task).
2. **Build the form** for the review (for example an input named `approved`, or fields that update the `Request`).
3. **Wire it via EAM:** operation `Complete a task` this time; the subject already exists, so nothing needs to be created. Declare the form fields as parameters exactly as before; parameter routing still applies: parameters that match `Request` properties update the subject, everything else becomes process parameter values.
4. **Add the visibility rule** for the matching state on the review form's container (`A task is available to claim` plus a claim button, or `A task is reserved by me` for a directly assigned reviewer).

Form, EAM wiring, visibility rule: that trio is the whole recipe, repeated once per user task.

## Next steps

- **Event Action Mapping**: the general mechanism behind all the wiring in this tutorial, including parameter mappings, follow-up actions and notifications.
- **Process operations**: beyond start and complete, tasks support `claim`, `release`, `decline`, `delegate` and administrative operations such as reassignment; whole processes can be suspended, resumed and terminated.
- **Subject and parameter handling**: the routing rule from the callout above in full detail, including how listeners can create the subject during completion.
- **Task and process event handlers**: run your own logic (notifications, audit, derived data) on lifecycle events such as `assigned` or `completed`, with pre-commit veto or post-commit side-effect semantics.
