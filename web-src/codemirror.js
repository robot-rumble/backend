import CodeMirror from 'codemirror'
import 'codemirror/mode/javascript/javascript.js'
import 'codemirror/lib/codemirror.css'

// inline loader syntax used because otherwise this loader doesn't work
// eslint-disable-next-line import/no-webpack-loader-syntax
import robotLib from '!raw-loader!./robotLib.raw'
import sampleRobot from '!raw-loader!./sampleRobot.raw'

export function initCodeMirror(changeCode) {
  const cm = CodeMirror(document.querySelector('#editor'), {
    tabSize: 2,
    mode: 'text/javascript',
    lineNumbers: true,
    matchBrackets: true,
    autoRefresh: true,
  })
  cm.setValue(sampleRobot)
  changeCode(processCode(sampleRobot))

  cm.setOption('extraKeys', {
    Tab: (cm) => cm.execCommand('indentMore'),
    'Shift-Tab': (cm) => cm.execCommand('indentLess'),
  })

  cm.on('change', () => {
    changeCode(processCode(cm.getValue()))
  })
}

function processCode(code) {
  return robotLib + ';' + code
}
