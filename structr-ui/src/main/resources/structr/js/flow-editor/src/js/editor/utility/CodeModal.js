import {DependencyLoader} from "./DependencyLoader.js";

export class CodeModal {

    constructor(rootElement) {
        if (rootElement !== undefined) {
            this._injectDependencies().then( () => {
                this._createEditorModal(rootElement);
            });
        } else {
            console.error("CodeModal: Given rootElement was undefined!");
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

        let editor = CodeMirror(container.querySelector("div.modal-content"), Structr.getCodeMirrorSettings({
            lineNumbers: true,
            styleActiveLine: true,
            matchBrackets: true,
            indentUnit: 4,
            indentWithTabs: true,
            scrollbarStyle: "overlay",
            value: element.value,
            autofocus: true,
            extraKeys: {
                "F11": function(cm) {
                    cm.setOption("fullScreen", !cm.getOption("fullScreen"));
                }
            }
        }));

        editor.setOption("theme", "darcula");

        // Write changes on blur/exit
        editor.on('blur', ()=> { element.value = editor.getValue(); element.dispatchEvent(new Event('change')); });

        // Stop key events from bubbling up
        editor.on('keydown', (event) => event.stopPropagation());

        // Focus editor and move cursor to end of last line
        editor.setCursor(editor.lineCount(), 0);
        editor.focus();
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

            dep.injectDependencies(depObject).then( () => {
                // Inject missing lib that depends on CodeMirror
                let depObject = {
                    scripts: [
                        "lib/codemirror/javascript/javascript.js",
                        "lib/codemirror/simplescrollbars.js",
                        "lib/codemirror/fullscreen.js"
                    ],
                    stylesheets: [
                        "lib/codemirror/simplescrollbars.css",
                        "lib/codemirror/fullscreen.css"
                    ]
                };
               return dep.injectDependencies(depObject);
            }).then( () => {
                resolve(true);
            });

        });
    }

}