import CodeMirror from 'codemirror'
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/lib/codemirror.css'

// inline loader syntax used because otherwise this loader doesn't work
// eslint-disable-next-line import/no-webpack-loader-syntax
import robotLib from '!raw-loader!./robotLib.raw'
import sampleRobot from '!raw-loader!./sampleRobot.raw'

customElements.define(
  'code-editor',
  class extends HTMLElement {
    constructor() {
      super()
      this._value = sampleRobot
    }

    get value() {
      return robotLib + ';' + this._value
    }

    connectedCallback() {
      this._editor = CodeMirror(this, {
        tabSize: 2,
        mode: 'text/javascript',
        lineNumbers: true,
        matchBrackets: true,
        autoRefresh: true,
        value: this._value,
        extraKeys: {
          Tab: (cm) => cm.execCommand('indentMore'),
          'Shift-Tab': (cm) => cm.execCommand('indentLess'),
        },
      })

      this._editor.on('changes', () => {
        this._value = this._editor.getValue()
        this.dispatchEvent(new CustomEvent('editorChanged'))
      })

      this.dispatchEvent(new CustomEvent('editorChanged'))
    }
  },
)
