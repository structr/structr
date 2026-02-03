## Overview

Most of the processes in Structr are event-driven: things happen when database objects are created, modified or deleted. For these events, Structr provides lifecycle methods that you can implement on any type in the data model, using either StructrScript or Javascript.

In addition to the lifecycle methods, you can of course add arbitrary methods to any type, and execute them

- from within your web application using Event Action Mapping and
- from other scripts
- via REST

The third option to implement logic is to use a global schema method, especially when the logic is not bound to a specific type. Global schema methods can be called via REST using a special maintenance endpoint, scheduled for execution with a cron-like syntax or called from any scripting environment using the call() or call_privileged() functions.







#### Things to include

- How to pass arguments to script calls
- How to access the arguments (`$.arguments`)
- how to call super implementations
- automatic calling of super implementations in Lifecycle Methods
- modifications in onSave etc.