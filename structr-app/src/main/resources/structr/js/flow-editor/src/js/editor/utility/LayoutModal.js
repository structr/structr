import {LayoutManager} from "./LayoutManager.js";
import {Persistence} from "../../../../../lib/structr/persistence/Persistence.js";

export class LayoutModal {

    constructor( editor) {
        this._editor = editor;

        let modal = document.querySelector("div#layoutModal");

        if (modal === undefined || modal === null) {

            this._createLayoutModal();

        }
    }

    async _createLayoutModal() {

        let layoutManager = new LayoutManager(this._editor);

        let container = document.createElement("div");
        container.classList.add("modal");
        container.setAttribute("id", "layoutModal");

        let layouts = await layoutManager.getSavedLayouts(true);

        let optionsMarkup = null;

        let activeLayoutId = this._editor._flowContainer.activeConfiguration !== null && this._editor._flowContainer.activeConfiguration !== undefined ? this._editor._flowContainer.activeConfiguration.id : null;

        if (layouts !== null && layouts.length > 0) {
            optionsMarkup = `
                ${layouts.map(layout => '<option value="' + layout.id + '" ' + (layout.id === activeLayoutId ? 'selected' : '') + '>' + (layout.name !== null ? layout.name : layout.id) + (layout.id === activeLayoutId ? ' (active)' : '') + '</option>').join('')}
            `;
        }

        container.innerHTML = `
              <div class="modal-content">
                <span class="close">&times;</span>
                <div class="layout-controls">
                    <h2>Layouts</h2>
                    <div class="inline">
                        <select id="layout-select">${optionsMarkup !== null ? optionsMarkup : ''}</select>
                        <button id="layout-select-btn">Apply & Set active</button>
                    </div>
                    <hr>
                    <div class="inline">
                        <button id="layout-save-btn">Save current layout</button>
                        <button id="layout-save-new-btn">Save new layout</button>
                        <button id="layout-rename-btn">Rename layout</button>
                        <button id="layout-delete-btn">Delete layout</button>
                        <div class="inline input-group">
                            <input id="layout-visibility-checkbox" type="checkbox" checked="true">
                            <label for="layout-visibility-checkbox">Visible for all Users</label>
                        </div>
                    </div>
                </div>
              </div>
        `;

        container.style.display = "block";

        container.querySelector("span.close").addEventListener('click', () => {
            container.remove();
        });
        document.body.append(container);

        // Make modal closable via ESC key
        container.addEventListener('keyup', (ev) => {
            if(ev.key === "Escape") {
                container.remove();
            }
        });

        container.querySelector("button#layout-save-btn").addEventListener('click', ()=> {
            let layoutManager = new LayoutManager(this._editor);
            const select = container.querySelector("select#layout-select");
            const checkbox = container.querySelector("input#layout-visibility-checkbox");
            if(select.selectedIndex !== -1) {
                if (confirm('Overwrite currently saved layout?')) {
                    layoutManager.saveLayout(checkbox.checked, false);
                    container.remove();
                }
            } else {
                layoutManager.saveLayout(checkbox.checked, false);
                container.remove();
            }

        });

		container.querySelector("button#layout-save-new-btn").addEventListener('click', ()=> {
			let layoutManager = new LayoutManager(this._editor);
			const select = container.querySelector("select#layout-select");
			const checkbox = container.querySelector("input#layout-visibility-checkbox");

			let name = prompt("Enter layout name", "unnamed");

			let self = this;

			layoutManager.saveLayout(checkbox.checked, true).then((config) => {
				config = config[0];
				config.name = name;
				this._editor._flowContainer.activeConfiguration = config;
				container.remove();
			});
		});


        container.querySelector("button#layout-select-btn").addEventListener('click', () => {
            let persistence = new Persistence();
            let layoutManager = new LayoutManager(this._editor);
            let selectedId = container.querySelector("select#layout-select").value;

            persistence.getNodesById(selectedId, {type: 'FlowContainerConfiguration'}, 'all').then(r => {
                if (r !== null && r !== undefined && r.length > 0) {
					this._editor._flowContainer.activeConfiguration = r[0].id;
					let currentText = container.querySelector("select#layout-select > option:checked").text;
					if (currentText.indexOf('(active)') === -1) {
						let options = container.querySelector("select#layout-select").options;
						for (let option of options) {
							option.text = option.text.replace(" (active)", "");
						}
						container.querySelector("select#layout-select > option:checked").text +=  ' (active)';
					}
				}
            });

            layoutManager.getSavedLayoutById(selectedId).then( (r) => {
                layoutManager.applySavedLayout(r);
            });

        });

		container.querySelector("button#layout-delete-btn").addEventListener('click', () => {
			if (confirm("Delete currently selected layout?")) {
				let persistence = new Persistence();
				let selectedId = container.querySelector("select#layout-select").value;

				persistence.getNodesById(selectedId, {type: 'FlowContainerConfiguration'}, 'all').then(r => {
					if (r !== null && r !== undefined && r.length > 0) {
						persistence.deleteNode(r[0]);
						const select = container.querySelector("select#layout-select");
						select.remove(select.selectedIndex);
					}
				});
			}

		});

		container.querySelector("button#layout-rename-btn").addEventListener('click',() => {
			let name = prompt("Enter new layout name", "");
			if (name !== null && name !== undefined && name.length > 0) {
			    let persistence = new Persistence();
				let selectedId = container.querySelector("select#layout-select").value;
				persistence.getNodesById(selectedId, {type: 'FlowContainerConfiguration'}, 'all').then(config => {
					if (config !== null && config !== undefined && config.length > 0) {
						config = config[0];
						config.name = name;
						let isActive = config.activeForFlow !== null && config.activeForFlow !== undefined ? config.activeForFlow.id === this._editor._flowContainer.id : false;
						container.querySelector("select#layout-select > option:checked").text = isActive ? (name + ' (active)') : name;
					}
                });
            }

		});

        container.querySelector("button#layout-save-btn").focus();

    }


}