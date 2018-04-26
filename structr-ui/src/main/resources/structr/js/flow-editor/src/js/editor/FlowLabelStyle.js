import {LabelStyleBase, Size, SvgVisual} from "../../lib/yfiles/view-component.js";

export class FlowLabelStyle extends LabelStyleBase {
    /**
     * @param {yfiles.view.Font} font
     */
    constructor(font) {
        super();
        this.fontField = font;
    }

    /**
     * Creates a visual that uses a foreignObject-element to display a HTML formatted text.
     * @see Overrides {@link yfiles.styles.LabelStyleBase#createVisual}
     * @param {yfiles.view.IRenderContext} context
     * @param {yfiles.graph.ILabel} label
     * @return {yfiles.view.SvgVisual}
     */
    createVisual(context, label) {
        const labelLayout = label.layout;

        const foreignObject = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
        foreignObject.setAttribute('x', '0');
        foreignObject.setAttribute('y', '0');

        const div = document.createElement('div');
        div.style.setProperty('overflow', 'hidden', '');
        foreignObject.setAttribute('width', `${labelLayout.width}`);
        foreignObject.setAttribute('height', `${labelLayout.height}`);
        div.style.setProperty('width', `${labelLayout.width}px`, '');
        div.style.setProperty('height', `${labelLayout.height}px`, '');
        div.style.setProperty('font-family', this.font.fontFamily, '');
        div.style.setProperty('font-size', `${this.font.fontSize}px`, '');
        div.style.setProperty('font-weight', `${this.font.fontWeight}`, '');
        div.style.setProperty('font-style', `${this.font.fontStyle}`, '');
        div.innerHTML = label.text;
        foreignObject.appendChild(div);

        // move container to correct location
        const transform = LabelStyleBase.createLayoutTransform(label.layout, true);
        transform.applyTo(foreignObject);

        // Get the necessary data for rendering of the label and store information with the visual
        foreignObject['data-cache'] = FlowLabelStyle.createRenderDataCache(label, this.font);

        return new SvgVisual(foreignObject);
    }

    /**
     * Updates the visual that uses a foreignObject-element to display a HTML formatted text.
     * @see Overrides {@link yfiles.styles.LabelStyleBase#updateVisual}
     * @param {yfiles.view.IRenderContext} context
     * @param {yfiles.view.SvgVisual} oldVisual
     * @param {yfiles.graph.ILabel} label
     * @return {yfiles.view.SvgVisual}
     */
    updateVisual(context, oldVisual, label) {
        const element = oldVisual.svgElement;
        if (element === null || element.childElementCount !== 1) {
            // re-create from scratch if this is not the case
            return this.createVisual(context, label);
        }

        // get the data with which the old visual was created
        const oldCache = element['data-cache'];

        // get the data for the new visual
        const newCache = FlowLabelStyle.createRenderDataCache(label, this.font);

        // update elements if they have changed
        const foreignObject = element;
        const div = foreignObject.firstElementChild;
        if (!oldCache.layout.equals(newCache.layout)) {
            const labelLayout = label.layout;
            foreignObject.setAttribute('width', `${labelLayout.width}`);
            foreignObject.setAttribute('height', `${labelLayout.height}`);

            div.style.setProperty('width', `${labelLayout.width}px`, '');
            div.style.setProperty('height', `${labelLayout.height}px`, '');
        }
        if (!oldCache.font.equals(newCache.font)) {
            div.style.setProperty('font-family', this.font.fontFamily, '');
            div.style.setProperty('font-size', `${this.font.fontSize}px`, '');
            div.style.setProperty('font-weight', `${this.font.fontWeight}`, '');
            div.style.setProperty('font-style', `${this.font.fontStyle}`, '');
        }

        if (oldCache.labelText !== newCache.labelText) {
            div.innerHTML = label.text;
        }

        // update the cache
        element['data-cache'] = newCache;

        // move container to correct location
        const transform = LabelStyleBase.createLayoutTransform(label.layout, true);
        transform.applyTo(foreignObject);

        return oldVisual;
    }

    /**
     * Returns the preferred size of the label.
     * @see Overrides {@link yfiles.styles.LabelStyleBase#getPreferredSize}
     * @param {yfiles.graph.ILabel} label The label to which this style instance is assigned.
     * @return {yfiles.geometry.Size} The preferred size.
     */
    getPreferredSize(label) {
        const div = document.createElement('div');
        div.style.setProperty('display', 'inline-block', '');
        div.innerHTML = label.text;
        document.body.appendChild(div);
        const clientRect = div.getBoundingClientRect();
        document.body.removeChild(div);
        return new Size(clientRect.width, clientRect.height);
    }

    /**
     * Creates an object containing all necessary data to create a label visual.
     * @param {yfiles.graph.ILabel} label The current label.
     * @param {yfiles.view.Font} font The font of the label text.
     * @return {Object}
     */
    static createRenderDataCache(label, font) {
        return { text: label.text, font, layout: label.layout };
    }

    /**
     * Returns the font used for rendering the label text.
     * @type {yfiles.view.Font}
     */
    get font() {
        return this.fontField;
    }

    /**
     * Specifies the font used for rendering the label text.
     * @type {yfiles.view.Font}
     */
    set font(value) {
        this.fontField = value;
    }
}
