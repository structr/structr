export class ResultPanel {

    constructor(result) {
        this._result = result;
        this._createLayoutModal();
    }

    async _createLayoutModal() {

        let container = document.createElement("div");
        container.setAttribute("id", "executionResult");

        const result = JSON.stringify(this._result, null, 2);

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

        document.body.append(container);

        // Make modal closable via ESC key
        container.addEventListener('keyup', (ev) => {
            if(ev.key === "Escape") {
                container.remove();
            }
        });

    }


}