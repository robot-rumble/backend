import CodeMirror from 'codemirror'
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/mode/python/python.js'
import 'codemirror/lib/codemirror.css'

// inline loader syntax used because otherwise this loader doesn't work
// eslint-disable-next-line import/no-webpack-loader-syntax
// import robotLib from '!raw-loader!./robotLib.raw'
// import sampleRobot from '!raw-loader!./sampleRobot.raw'

import sampleRobot from './robots/sample.raw.py'

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
      this.marks = []
      this.lastRunCount = 0
    }

    clearMarks() {
      this.marks.forEach((mark) => mark.clear())
      this.marks = []
    }

    set errorLoc(errorLoc) {
      if (errorLoc && window.runCount !== this.lastRunCount) {
        this.lastRunCount = window.runCount

        let from = { line: errorLoc.line - 1, ch: errorLoc.ch - 1 }
        let to = { line: errorLoc.endline - 1, ch: errorLoc.endch - 1 }

        let mark = this._editor.markText(from, to, {
          className: 'inline-error',
        })

        // error is in area that doesn't have a character, eg no colon in python function definition
        if (!mark.lines.length) {
          this._editor.replaceRange(' ', from, to)
          mark = this._editor.markText(from, to, {
            className: 'inline-error',
          })
        }

        this.marks.push(mark)
      }
    }

    get value() {
      return this._editor.getValue()
    }

    connectedCallback() {
      let localSave = JSON.parse(localStorage.getItem('code_' + this.name))
      let localCode = localSave ? localSave.code : ''
      let localLastEdit = localSave ? localSave.lastEdit : 0

      let initialValue
      if (this.code && localCode) {
        initialValue = this.lastEdit > localLastEdit ? this.code : localCode
      } else {
        initialValue = this.code || localCode || sampleRobot
      }

      this._editor = CodeMirror(this, {
        tabSize: 2,
        mode: getModeFromLanguage(window.language),
        lineNumbers: true,
        matchBrackets: true,
        autoRefresh: true,
        value: initialValue,
        extraKeys: {
          Tab: (cm) => cm.execCommand('indentMore'),
          'Shift-Tab': (cm) => cm.execCommand('indentLess'),
        },
      })

      this._editor.on('changes', () => {
        this.clearMarks()
        localStorage.setItem(
          'code_' + this.name,
          JSON.stringify({
            code: this._editor.getValue(),
            lastEdit: Math.floor(Date.now() / 1000),
          }),
        )
        this.dispatchEvent(new CustomEvent('editorChanged'))
      })

      this.dispatchEvent(new CustomEvent('editorChanged'))

      document.fonts.ready.then(() => {
        if (this._editor) this._editor.refresh()
      })
    }
  },
)
