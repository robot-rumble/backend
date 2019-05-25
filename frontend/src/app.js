import { Elm } from './Main.elm'
import './css/app.scss'

import './codemirror'

window.language = 'python'
window.turnNum = 10

let app = Elm.Main.init({
  node: document.getElementById('root'),
  flags: {
    totalTurns: window.turnNum,
  },
})

const matchWorker = new Worker('./worker.js')

app.ports.startEval.subscribe((code) => {
  localStorage.setItem('code', code)
  matchWorker.postMessage(code)
})

matchWorker.onmessage = ({ data }) => {
  if (data.type === 'error') {
    console.log('Worker Error')
    console.error(data.data)
  } else if (data.type === 'robotError') {
    console.log('Robot Error')
    console.log(data.data)
  } else {
    console.log(app.ports)
    app.ports[data.type].send(data.data)
  }
}
