import {DependencyLoader} from "./DependencyLoader.js";

export class CodeModal {

    constructor(rootElement) {
        if (rootElement !== undefined) {
            this._injectDependencies().then( () => {
                this._createEditorModal(rootElement);
            });
        } else {
            console.error("CodeModa: Given rootElement was undefined!");
        }
    }

    _createEditorModal(element) {

        let container = document.createElement("div");
        container.classList.add("modal");
        container.setAttribute("id", "editorModal");

        const markup = `
              <div class="modal-content">
                <span class="close">&times;</span>
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

        let editor = CodeMirror(container.querySelector("div.modal-content"), {
            lineNumbers: true,
            styleActiveLine: true,
            matchBrackets: true,
            value: element.value
        });

        editor.setOption("theme", "darcula");

        editor.on('blur', ()=>{element.value = editor.getValue();element.dispatchEvent(new Event('change'))});

    }

    _injectDependencies() {
        return new Promise(function(resolve) {
            const dep = new DependencyLoader();

            const depObject = {
                scripts: [
                    "lib/codemirror/codemirror.js"
                ],
                stylesheets: [
                    "lib/codemirror/codemirror.css",
                    "lib/codemirror/darcula.css"
                ]
            };

            dep._injectDependencies(depObject).then( () => {
                // Inject missing lib that depends on CodeMirror
               return dep._injectScript("lib/codemirror/javascript/javascript.js");
            }).then( () => {
                resolve(true);
            });

        });
    }

}