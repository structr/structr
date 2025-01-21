/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
document.addEventListener("DOMContentLoaded", () => {
    Structr.registerModule(_Processes);
    //Structr.classes.push('process');
});

let _Processes = {
    visualization: {
        circleSize: 24,
        squareWidth: 36,
        squareHeight: 24,
        rasterX: 144,
        marginLeft: -96
    },
    processes: [], // collect displayed processes
    process: processId => {
        if (_Processes.processes.length) {
            const currentProcess = _Processes.processes.filter(p => p.id === processId)[0];
            if (!currentProcess.states)    currentProcess.states = [];
            if (!currentProcess.steps)     currentProcess.steps = [];
            if (!currentProcess.decisions) currentProcess.decisions = [];
            return currentProcess;
        }
    },
    _moduleName: 'processes',
    init: () => {
        console.log('init');
        Structr.adaptUiToAvailableFeatures();
        document.getElementById('create-process').addEventListener('click', _Processes.ui.showCreateProcessDialog);
        _Processes.reload();
    },
    onload: async (retryCount = 0) => {
        console.log('onload');
        Structr.setMainContainerHTML(_Processes.templates.main());
        Structr.setFunctionBarHTML(_Processes.templates.functions());
        Structr.mainMenu.unblock(100);
        _Processes.init();
    },
    reload: () => {
        console.log('reload');
        _Processes.processes = [];
        _Helpers.fastRemoveAllChildren(Structr.mainContainer);
        Structr.setMainContainerHTML(_Processes.templates.main());
            Command.getByType('Process', 1000, 1, 'name', null, 'id,name,firstStep,initialState', false, processes => {
            processes.forEach(process => {
                _Processes.displayProcess(process);
            });
        });
    },
    addProcess: data => {
        data.type = 'Process';
        Command.create(data, () => {
            _Processes.reload();
        });
    },
    addState: data => {
        data.type = 'ProcessState';
        Command.create(data, () => {
            _Processes.reload();
        });
    },
    addDecision: data => {
        data.type = 'ProcessDecision';
        Command.create(data, () => {
            _Processes.reload();
        });
    },
    addStep: data => {
        data.type = 'ProcessStep';
        Command.create(data, () => {
            _Processes.reload();
        });
    },
    deleteStep: id => {
        Command.deleteNode(id, false, () => {
           _Processes.reload();
        });
    },
    displayProcess: process => {
        _Processes.processes.push(process);
        Structr.mainContainer.insertAdjacentHTML('beforeend', `
            <div id="process-${process.id}" class="relative" style="width:calc(100vw - 6rem); height:24rem; margin:4rem 0 0 2rem;">
                <h2>${process.name}</h2>
            </div>`);
        const processContainer = document.getElementById(`process-${process.id}`);
        if (process.initialState) {
            _Processes.displayState(process, process.initialState);
        } else {
            processContainer.insertAdjacentHTML('beforeend', `<button class="add-state absolute border-0 inline-flex items-center icon-inactive hover:icon-active" style="top:3rem;left:0rem" data-process-id="${process.id}">
            ${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['add', 'mr-1'])} Add initial state</button>`);

            document.querySelector('.add-state[data-process-id]').addEventListener('click', e => {
                const btn = e.target.closest('[data-process-id]');
                _Processes.ui.showCreateProcessStateDialog(null, btn.dataset['processId'], true);
            });
        }
    },
    displayState: (process, state) => {
        const processContainer = document.getElementById(`process-${process.id}`);

        //processContainer.insertAdjacentHTML('beforeend', `<div class="process-arrow-right" style="left:${coords[0]*_Processes.visualization.squareWidth+3}rem; top:${coords[1]*_Processes.visualization.squareHeight/2+7.5}rem"></div>`);
        //console.log(JSON.stringify(_Processes.states));

        if (state && !_Processes.process(process.id).states.includes(state.id)) {
            _Processes.process(process.id).states.push(state.id);

            let x = _Processes.process(process.id).steps.length + 1; // + (_Processes.states.length % 3 > 0 ? 1 : 0);
            const success = (!state.status || state.status < 300);

            //console.log('Displaying state', x, ', name:', state.name, ', status: ', state.status, ', success: ', success);

            processContainer.insertAdjacentHTML('beforeend', `<div class="absolute shadow rounded-full w-${_Processes.visualization.circleSize} h-${_Processes.visualization.circleSize} flex flex-row text-center items-center justify-center border border-gray-666 bg-white" 
            style="left:${(_Processes.visualization.marginLeft/4) + x * (_Processes.visualization.rasterX/4) - 2*(_Processes.visualization.circleSize/4) - (!success ? 2*(_Processes.visualization.circleSize/4) - 1.2 : 0)}rem;
            top:${(success ? 0 : 1) *_Processes.visualization.squareHeight/2+(success ? 4.8 : 3.7)}rem;">${state.name.length>21 ? state.name.substring(0,21) + '…' : state.name}</div>`);

            let s = 0;
            Command.get(state.id, 'id,name,nextStep', stateData => {

                if (stateData.nextStep) {
                    _Processes.displayStep(process, stateData.nextStep);
                } else {

                    processContainer.insertAdjacentHTML('beforeend', `<button class="add-step absolute border-0 inline-flex items-center icon-inactive hover:icon-active" style="
                    left:${(_Processes.visualization.marginLeft / 4) + x * (_Processes.visualization.rasterX / 4) - 4}rem;
                    top:${(success ? 0 : 1) * _Processes.visualization.squareHeight / 2 + 6.5}rem;" data-state-id="${state.id}">
                    ${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['add', 'mr-1'])} Add step</button>`);

                    document.querySelector(`.add-step[data-state-id="${state.id}"]`).addEventListener('click', e => {
                        const btn = e.target.closest(`[data-state-id="${state.id}"]`);
                        //console.log('Clicked on ', btn);
                        _Processes.ui.showCreateProcessStepDialog(btn.dataset['stateId'], process.id);
                    });
                }
            });
        }

    },
    displayStep: (process, step) => {
        const processContainer = document.getElementById(`process-${process.id}`);

        if (step && !_Processes.process(process.id).steps.includes(step.id)) {

            _Processes.process(process.id).steps.push(step.id);
            const x = _Processes.process(process.id).steps.length;

            processContainer.insertAdjacentHTML('beforeend', `<div class="process-arrow-right" style="left:${(_Processes.visualization.marginLeft/4) + x * (_Processes.visualization.rasterX/4) - (_Processes.visualization.circleSize/4)/2 - .5}rem; top:${0 * _Processes.visualization.squareHeight / 2 + 7.5}rem"></div>`);
            processContainer.insertAdjacentHTML('beforeend', `
            <div class="process-step-${step.id} absolute rounded-xl w-${_Processes.visualization.squareWidth} h-${_Processes.visualization.squareHeight} flex flex-row items-center text-center justify-center p-4 border border-gray-666 bg-white"
            style="left:${(_Processes.visualization.marginLeft/4) + x * (_Processes.visualization.rasterX/4) - 2.4}rem; top:4rem;">
                ${_Icons.getSvgIcon(_Icons.iconCrossIcon, 10, 10, ['delete-' + step.id, 'absolute', 'right-2', 'top-2', 'cursor-pointer', 'icon-inactive', 'hover:icon-active', 'hover:text-gray-666'])}
                ${step.name}
            </div>`);

            document.querySelector(`.delete-${step.id}`).addEventListener('click', e => {
                _Processes.deleteStep(step.id);
            });

            let s = 0;
            Command.get(step.id, 'id,name,parameters,decision', stepData => {

                if (stepData.parameters && stepData.parameters.length) {
                    let p=1;
                    stepData.parameters.forEach(param => {
                        //document.querySelector(`.process-step-${step.id}`)
                        processContainer.insertAdjacentHTML('beforeend', `<div class="absolute text-xs border border-gray-666 bg-white rounded px-1" style="z-index:1;
                        left:${(_Processes.visualization.marginLeft/4) + x * (_Processes.visualization.rasterX/4) + 8}rem;
                        top:${7+p*2}rem;">${param.name}</div>`);
                    });
                }

                if (stepData.decision) {
                    _Processes.displayDecision(process, stepData.decision);
                } else {
                    processContainer.insertAdjacentHTML('beforeend', `<button class="add-state absolute border-0 inline-flex items-center icon-inactive hover:icon-active" style="
                    left:${(_Processes.visualization.marginLeft / 4) + x * (_Processes.visualization.rasterX / 4) + 10}rem;
                    top:6.95rem;" data-step-id="${step.id}">${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['add', 'mr-1'])} Add decision</button>`);

                    document.querySelector(`.add-state[data-step-id="${step.id}"]`).addEventListener('click', e => {
                        const btn = e.target.closest(`[data-step-id="${step.id}"]`);
                        _Processes.showCreateProcessDecisionDialog(btn.dataset['stepId']);
                    });
                }
            });
        }
    },
    displayDecision: (process, decision) => {
        const processContainer = document.getElementById(`process-${process.id}`);

        if (decision && !_Processes.process(process.id).decisions.includes(decision.id)) {

            _Processes.process(process.id).decisions.push(decision.id);

            const x = _Processes.process(process.id).steps.length;

            processContainer.insertAdjacentHTML('beforeend', `<div class="process-arrow-right" style="
            left:${(_Processes.visualization.marginLeft/4) + x*(_Processes.visualization.rasterX/4) + (_Processes.visualization.squareWidth/4) + 2}rem;
            top:${0*_Processes.visualization.squareHeight/2+7.5}rem;"
            ></div>`);

            processContainer.insertAdjacentHTML('beforeend', `<div class="absolute rotate-45 w-${_Processes.visualization.circleSize} h-${_Processes.visualization.circleSize} flex flex-row text-center items-center justify-center border border-gray-666 bg-white" style="
                left:${(_Processes.visualization.marginLeft/4) + x*(_Processes.visualization.rasterX/4) + (_Processes.visualization.squareWidth/4) + 4.25}rem;
                top:${0*_Processes.visualization.squareHeight/4+4*1.22}rem;"
            ><div class="-rotate-45">${decision.name}</div></div>`);
            let s = 0;
            Command.get(decision.id, 'id,name,step,possibleStates', decisionData => {

                decisionData.possibleStates.forEach(state => {

                    Command.get(state.id, 'id,name,status', stateData => {

                        const success = (!stateData.status || stateData.status < 300);

                        processContainer.insertAdjacentHTML('beforeend', `<div class="process-arrow-${success ? 'right' : 'down'}" style="
                        left:${(_Processes.visualization.marginLeft/4) + x*(_Processes.visualization.rasterX/4) + (success ? 2.55 : 1.764)*(_Processes.visualization.squareWidth/4)}rem;
                        top:${(success ? 0 : 1) *_Processes.visualization.squareHeight/2+(success ? 7.5 : 2.6)}rem;"
                        ></div>`);

                        processContainer.insertAdjacentHTML('beforeend', `<div style="position:absolute;
                            left:${(_Processes.visualization.marginLeft/4) + x*(_Processes.visualization.rasterX/4) + (success ? 2.35 : 1.63)*(_Processes.visualization.squareWidth/4)}rem;
                            top:${(success ? 0 : 1) *_Processes.visualization.squareHeight/2+(success ? 6.45 : 1)}rem;">${success?'Yes':'No'}</div>`);

                        if (!success) {
                            processContainer.insertAdjacentHTML('beforeend', `<div class="process-line-left process-long-line-left" style="
                            left:${(_Processes.visualization.marginLeft/4) + x*(_Processes.visualization.rasterX/4) + 2.5}rem;
                            top:${_Processes.visualization.squareHeight/2+6.5}rem;"
                            ></div>`);
                            processContainer.insertAdjacentHTML('beforeend', `<div class="process-arrow-up process-long-arrow-up" style="
                            left:${(_Processes.visualization.marginLeft/4) + x*(_Processes.visualization.rasterX/4) + 2.5}rem;
                            top:${_Processes.visualization.squareHeight/2+.29}rem;"
                            ></div>`);
                        }

                        _Processes.displayState(process, stateData);

                    });
                });
/*
                processContainer.insertAdjacentHTML('beforeend', `<div class="process-arrow-down" style="
                left:31.85rem;
                top:${coords[1]*_Processes.visualization.squareHeight/2+14.6}rem;"
                ></div>`);
                _Processes.displayState(process, decisionData.possibleStates.filter(s => s.status >= 300), [coords[0]+3.25, s++]);

 */
            });
        }

    },
    resize: () => {
    },
    unload: () => {
    },
    ui: {
        showCreateNewProcessDialog: (container, updateFunction) => {
            container.insertAdjacentHTML('beforeend', _Processes.templates.createProcessDialog());
            _Helpers.activateCommentsInElement(container);
        },
        showCreateNewProcessStateDialog: (container, updateFunction) => {
            container.insertAdjacentHTML('beforeend', _Processes.templates.createProcessStateDialog());
            _Helpers.activateCommentsInElement(container);
        },
        showCreateNewProcessStepDialog: (container, updateFunction) => {
            container.insertAdjacentHTML('beforeend', _Processes.templates.createProcessStepDialog());
            _Helpers.activateCommentsInElement(container);
        },
        showCreateNewProcessDecisionDialog: (container, updateFunction) => {
            container.insertAdjacentHTML('beforeend', _Processes.templates.createProcessDecisionDialog());
            _Helpers.activateCommentsInElement(container);
        },
        showCreateProcessDialog: async () => {

            let {dialogText} = _Dialogs.custom.openDialog("Create process", null, ['process-edit-dialog']);

            let discardButton = _Dialogs.custom.prependCustomDialogButton(_Processes.templates.discardActionButton({text: 'Discard and close' }));
            let saveButton    = _Dialogs.custom.prependCustomDialogButton(_Processes.templates.saveAndCloseActionButton({ text: 'Create' }));
            _Processes.ui.showCreateNewProcessDialog(dialogText);

            _Dialogs.custom.setDialogSaveButton(saveButton);

            saveButton.addEventListener('click', async (e) => {

                let processData = _Code.collectDataFromContainer(dialogText, {});

                if (!processData.name || processData.name.trim() === '') {
                    _Helpers.blinkRed(dialogText.querySelector('[data-property="name"]'));
                } else {
                    _Processes.addProcess(processData);
                    _Dialogs.custom.dialogCancelBaseAction();
                }
            });

            // replace old cancel button with "discard button" to enable global ESC handler
            _Dialogs.custom.replaceDialogCloseButton(discardButton, false);
            discardButton.addEventListener('click', (e) => {
                _Dialogs.custom.dialogCancelBaseAction();
            });
        },
        showCreateProcessStateDialog: async (stepId, processId) => {

            let {dialogText} = _Dialogs.custom.openDialog("Create process state", null, ['process-state-edit-dialog']);

            let discardButton = _Dialogs.custom.prependCustomDialogButton(_Processes.templates.discardActionButton({text: 'Discard and close' }));
            let saveButton    = _Dialogs.custom.prependCustomDialogButton(_Processes.templates.saveAndCloseActionButton({ text: 'Create' }));

            _Processes.ui.showCreateNewProcessStateDialog(dialogText);

            _Dialogs.custom.setDialogSaveButton(saveButton);

            saveButton.addEventListener('click', async (e) => {
                let processStateData = _Code.collectDataFromContainer(dialogText, {});

                if (!processStateData.name || processStateData.name.trim() === '') {
                    _Helpers.blinkRed(dialogText.querySelector('[data-property="name"]'));
                } else {
                    if (stepId) {
                        processStateData.previousSteps = [stepId];
                    } else {
                        processStateData.processThisStateIsInitial = processId;
                    }
                    _Processes.addState(processStateData);
                    _Dialogs.custom.dialogCancelBaseAction();
                }
            });

            // replace old cancel button with "discard button" to enable global ESC handler
            _Dialogs.custom.replaceDialogCloseButton(discardButton, false);
            discardButton.addEventListener('click', (e) => {
                _Dialogs.custom.dialogCancelBaseAction();
            });
        },
        showCreateProcessStepDialog: async (stateId, processId) => {
            let {dialogText} = _Dialogs.custom.openDialog("Create process step", null, ['process-step-edit-dialog']);

            let discardButton = _Dialogs.custom.prependCustomDialogButton(_Processes.templates.discardActionButton({text: 'Discard and close' }));
            let saveButton    = _Dialogs.custom.prependCustomDialogButton(_Processes.templates.saveAndCloseActionButton({ text: 'Create' }));

            let initialData;

            _Processes.ui.showCreateNewProcessStepDialog(dialogText);

            _Dialogs.custom.setDialogSaveButton(saveButton);

            saveButton.addEventListener('click', async (e) => {
                let processStepData = _Code.collectDataFromContainer(dialogText, {});

                if (!processStepData.name || processStepData.name.trim() === '') {
                    _Helpers.blinkRed(dialogText.querySelector('[data-property="name"]'));
                } else {
                    processStepData.previousStates = [stateId];
                    processStepData.process = processId;
                    _Processes.addStep(processStepData);
                    _Dialogs.custom.dialogCancelBaseAction();
                }
            });

            // replace old cancel button with "discard button" to enable global ESC handler
            _Dialogs.custom.replaceDialogCloseButton(discardButton, false);
            discardButton.addEventListener('click', (e) => {
                _Dialogs.custom.dialogCancelBaseAction();
            });
        }
    },
    showCreateProcessDecisionDialog: async (stepId) => {

        let {dialogText} = _Dialogs.custom.openDialog("Create process decision", null, ['process-decision-edit-dialog']);

        let discardButton = _Dialogs.custom.prependCustomDialogButton(_Processes.templates.discardActionButton({text: 'Discard and close' }));
        let saveButton    = _Dialogs.custom.prependCustomDialogButton(_Processes.templates.saveAndCloseActionButton({ text: 'Create' }));
        _Processes.ui.showCreateNewProcessDecisionDialog(dialogText);

        _Editors.getMonacoEditor({}, "condition", dialogText.querySelector('.editor'), {
            language: 'auto',
            lint: true,
            autocomplete: true
        });

        _Dialogs.custom.setDialogSaveButton(saveButton);

        saveButton.addEventListener('click', async (e) => {
            let processDecisionData = _Code.collectDataFromContainer(dialogText, {});

            if (!processDecisionData.name || processDecisionData.name.trim() === '') {
                _Helpers.blinkRed(dialogText.querySelector('[data-property="name"]'));
            } else {

                const data = {
                    name: processDecisionData.name,
                    possibleStates: [
                        { type: 'ProcessState', name: processDecisionData.validState, status: 200 },
                        { type: 'ProcessState', name: processDecisionData.invalidState, status: 400, nextStep: stepId }
                    ],
                    condition: processDecisionData.condition,
                    step: stepId
                };

                _Processes.addDecision(data);
                _Dialogs.custom.dialogCancelBaseAction();
            }
        });

        // replace old cancel button with "discard button" to enable global ESC handler
        _Dialogs.custom.replaceDialogCloseButton(discardButton, false);
        discardButton.addEventListener('click', (e) => {
            _Dialogs.custom.dialogCancelBaseAction();
        });
    },

    templates: {
        main: config => `
            <link rel="stylesheet" type="text/css" media="screen" href="css/processes.css">
        `,
        functions: config => `
            <div class="flex-grow">
                <div class="inline-flex">

                    <button id="create-process" class="action inline-flex items-center">
                        ${_Icons.getSvgIcon(_Icons.iconAdd, 16, 16, ['mr-2'])} Create Process
                    </button>
                </div>
            </div>

			<div class="searchBox">
				<input id="processes-search-box" class="search" name="processes-search" placeholder="Search">
				${_Icons.getSvgIcon(_Icons.iconCrossIcon, 12, 12, _Icons.getSvgIconClassesForColoredIcon(['clearSearchIcon', 'icon-lightgrey', 'cursor-pointer']), 'Clear Search')}
			</div>
		`,
        createProcessDialog: config => `
			<div class="schema-details pl-2">
				<div class="flex items-center gap-x-2 pt-4">

					<input data-property="name" class="flex-grow" placeholder="Process Name...">

				</div>

				<h3>Options</h3>
				<div class="property-options-group">
					<div>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to public users if checked">
							<input id="public-checkbox" type="checkbox" data-property="defaultVisibleToPublic"> Visible for public users
						</label>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to authenticated users if checked">
							<input id="authenticated-checkbox" type="checkbox" data-property="defaultVisibleToAuth"> Visible for authenticated users
						</label>
					</div>
				</div>

			</div>
		`,
        createProcessStateDialog: config => `
			<div class="schema-details pl-2">
				<div class="flex items-center gap-x-2 pt-4">

					<input data-property="name" class="flex-grow" placeholder="Process State Name...">

				</div>

				<h3>Options</h3>
				<div class="property-options-group">
					<div>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to public users if checked">
							<input id="public-checkbox" type="checkbox" data-property="defaultVisibleToPublic"> Visible for public users
						</label>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to authenticated users if checked">
							<input id="authenticated-checkbox" type="checkbox" data-property="defaultVisibleToAuth"> Visible for authenticated users
						</label>
					</div>
				</div>

			</div>
		`,
        createProcessStepDialog: config => `
			<div class="schema-details pl-2">
				<div class="flex items-center gap-x-2 pt-4">

					<input data-property="name" class="flex-grow" placeholder="Process Step Name...">

				</div>

				<h3>Options</h3>
				<div class="property-options-group">
					<div>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to public users if checked">
							<input id="public-checkbox" type="checkbox" data-property="defaultVisibleToPublic"> Visible for public users
						</label>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to authenticated users if checked">
							<input id="authenticated-checkbox" type="checkbox" data-property="defaultVisibleToAuth"> Visible for authenticated users
						</label>
					</div>
				</div>

			</div>
		`,
        createProcessDecisionDialog: config => `
			<div class="schema-details pl-2">
			    <h2>Create Decision</h2>
				<div class="mt-4">
					<input data-property="name" class="flex-grow" placeholder="Process decision name...">
				</div>
				<div class="mt-4 w-full">
				    <label class="block font-bold">Condition</label>
					<div class="editor mt-2 h-24 w-full" data-property="condition" placeholder="Condition..." rows="10"></div>
				</div>
				<div class="mt-4">
					<input data-property="validState" class="flex-grow" placeholder="Positive/valid state...">
				</div>
				<div class="mt-4">
					<input data-property="invalidState" class="flex-grow" placeholder="Negative/invalid state...">
				</div>

				<h3>Options</h3>
				<div class="property-options-group">
					<div>
						<label class="" data-comment="Makes all nodes of this type visible to public users if checked">
							<input id="public-checkbox" type="checkbox" data-property="defaultVisibleToPublic"> Visible for public users
						</label>
						<label class="ml-8" data-comment="Makes all nodes of this type visible to authenticated users if checked">
							<input id="authenticated-checkbox" type="checkbox" data-property="defaultVisibleToAuth"> Visible for authenticated users
						</label>
					</div>
				</div>

			</div>
		`,
        saveAndCloseActionButton: config => `
            <button id="save-entity-button" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
                ${_Icons.getSvgIcon(_Icons.iconCheckmarkBold, 14, 14, ['icon-green', 'mr-2'])} ${(config?.text ?? 'Save')}
            </button>
        `,
        discardActionButton: config => `
            <button id="discard-entity-changes-button" class="inline-flex items-center hover:bg-gray-100 focus:border-gray-666 active:border-green">
                ${_Icons.getSvgIcon(_Icons.iconCrossIcon, 14, 14, ['icon-red', 'mr-2'])} ${(config?.text ?? 'Discard')}
            </button>
        `
    }
};