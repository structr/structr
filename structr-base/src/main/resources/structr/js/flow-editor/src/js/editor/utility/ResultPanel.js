export class ResultPanel {

    constructor(result, editor) {
        this._result = result;
        this._editor = editor;
        const formattedResult = JSON.stringify(this._result, null, 2);
        // Check for existing layout modal to hijack before creating a new one
        let container = document.body.querySelector("div#executionResult");

        if (container !== null && container !== undefined) {
            container.querySelector("pre").innerHTML = formattedResult;
        } else {
            this._createResultPanel(formattedResult);
        }
    }

    static removePanel() {
        let panel = document.body.querySelector("div#executionResult");
        if (panel !== undefined && panel !== null) {
            panel.parentNode.removeChild(panel);
        }
    }

    _createResultPanel(result) {

        let container = document.createElement("div");
        container.setAttribute("id", "executionResult");

        container.innerHTML = `
              <div class="modal-content">
                <span class="close">&times;</span>
                <div>
                    <h4>Execution Result</h4>
                    <pre class="result-panel">${result}</pre>
                </div>
              </div>
        `;

        container.style.display = "block";

        container.querySelector("span.close").addEventListener('click', () => {
            container.remove();
        });

        this._editor._rootElement.parentNode.prepend(container);

    }


}