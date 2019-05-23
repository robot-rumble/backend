import CodeMirror from 'codemirror'
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/mode/python/python.js'
import 'codemirror/lib/codemirror.css'

// inline loader syntax used because otherwise this loader doesn't work
// eslint-disable-next-line import/no-webpack-loader-syntax
// import robotLib from '!raw-loader!./robotLib.raw'
// import sampleRobot from '!raw-loader!./sampleRobot.raw'
//
import sampleRobot from '!raw-loader!./sampleRobot.raw.py'

function getModeFromLanguage(language) {
  switch (language) {
    case 'javascript':
      return 'text/javascript'
    case 'python':
      return 'python'
  }
}

customElements.define(
  'code-editor',
  class extends HTMLElement {
    constructor() {
      super()
      this._value = localStorage.getItem('code') || sampleRobot
    }

    // get value() {
    //   return robotLib + ';' + this._value
    // }

    get value() {
      return this._value
    }

    connectedCallback() {
      this._editor = CodeMirror(this, {
        tabSize: 2,
        mode: getModeFromLanguage(window.language),
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

      document.fonts.ready.then(() => {
        if (this._editor) this._editor.refresh()
      })
    }
  },
)
