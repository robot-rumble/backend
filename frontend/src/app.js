import { Elm } from './Main.elm'
import './css/app.scss'

import './codemirror'

window.turnNum = 10
window.language = 'python'
window.runCount = 0

const app = Elm.Main.init({
  node: document.getElementById('root'),
  flags: {
    totalTurns: window.turnNum,
  },
})

const matchWorker = new Worker('./worker.js')

app.ports.startEval.subscribe((code) => {
  localStorage.setItem('code', code)
  window.runCount++
  matchWorker.postMessage({ code, turnNum: window.turnNum })
})

matchWorker.onmessage = ({ data }) => {
  if (data.type === 'error') {
    console.log('Worker Error!')
    console.error(data.data)
  } else {
    if (data.type === 'getOutput') console.log(data.data)
    app.ports[data.type].send(data.data)
  }
}

app.ports.reportDecodeError.subscribe((error) => {
  console.log('Decode Error!')
  console.error(error)
})
