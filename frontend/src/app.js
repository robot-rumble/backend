import { Elm } from './Main.elm'
import './css/app.scss'

import './codemirror'

window.turnNum = 10
window.language = 'python'

const app = Elm.Main.init({
  node: document.getElementById('root'),
  flags: {
    totalTurns: window.turnNum,
  },
})

const matchWorker = new Worker('./worker.js')

app.ports.startEval.subscribe((code) => {
  localStorage.setItem('code', code)
  matchWorker.postMessage({ code, turnNum: window.turnNum })
})

matchWorker.onmessage = ({ data }) => {
  console.log(data)
  if (data.type === 'error') {
    console.log('Worker Error')
    console.error(data.data)
  } else if (data.type === 'robotError') {
    console.log('Robot Error')
    console.log(data.data)
  } else {
    app.ports[data.type].send(data.data)
  }
}
