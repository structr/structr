'use strict';

import { Persistence } from "./persistence/Persistence.js";
import { FlowContainer } from "./flow/model/FlowContainer.js";

window._flowPersistence = new Persistence();
window._flowContainerClass = FlowContainer;

window.result = undefined;