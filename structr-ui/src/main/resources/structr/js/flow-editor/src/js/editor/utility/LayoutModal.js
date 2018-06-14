import {LayoutManager} from "./LayoutManager.js";

export class LayoutModal {

    constructor( editor) {
        this._editor = editor;
        this._createLayoutModal();
    }

    async _createLayoutModal() {

        let layoutManager = new LayoutManager(this._editor);

        let container = document.createElement("div");
        container.classList.add("modal");
        container.setAttribute("id", "layoutModal");

        let layouts = await layoutManager.getSavedLayouts(true);

        let optionsMarkup = null;

        if (layouts !== null && layouts.length > 0) {
            optionsMarkup = `
                ${layouts.map(layout => '<option value="' + layout.id + '">' + (layout.principal !== null ? layout.principal.name : layout.id) + '</option>').join('')}
            `;
        }

        const markup = `
              <div class="modal-content">
                <span class="close">&times;</span>
                <div class="layout-controls">
                    <h2>Layouts</h2>
                    <div class="inline">
                        <select id="layout-select">${optionsMarkup !== null ? optionsMarkup : ''}</select>
                        <button id="layout-select-btn">Apply</button>
                    </div>
                    <hr>
                    <div class="inline">
                        <button id="layout-save-btn">Save current layout</button>
                    </div>
                </div>
              </div>
        `;

        container.innerHTML = markup;
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
            if (confirm('Overwrite currently saved layout?')) {
                layoutManager.saveLayout();
                container.remove();
            }
        });

        container.querySelector("button#layout-select-btn").addEventListener('click', () => {
            let layoutManager = new LayoutManager(this._editor);
            let selectedId = container.querySelector("select#layout-select").value;
            layoutManager.getSavedLayoutById(selectedId).then( (r) => {
                layoutManager.applySavedLayout(r);
            });

        });

        container.querySelector("div.modal-content");

    }


}