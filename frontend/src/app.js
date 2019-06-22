import { Elm } from './Main.elm'
import './css/app.scss'

import './codemirror'

window.turnNum = 10
window.language = 'python'
window.runCount = 0

let auth
try {
  auth = JSON.parse(localStorage.getItem('auth'))
} catch (e) {
  localStorage.removeItem('auth')
}

const app = Elm.Main.init({
  node: document.getElementById('root'),
  flags: {
    totalTurns: window.turnNum,
    auth,
  },
})

const matchWorker = new Worker('/worker.js')

app.ports.startEval.subscribe((code) => {
  window.runCount++
  matchWorker.postMessage({ code, turnNum: window.turnNum })
})

matchWorker.onmessage = ({ data }) => {
  if (data.type === 'error') {
    console.log('Worker Error!')
    console.error(data.data)
    app.ports.getError.send(null)
  } else {
    if (data.type === 'getOutput') console.log(data.data)
    app.ports[data.type].send(data.data)
  }
}

app.ports.reportDecodeError.subscribe((error) => {
  console.log('Decode Error!')
  console.error(error)
})

app.ports.storeAuth.subscribe((auth) => {
  localStorage.setItem('auth', JSON.stringify(auth))
})

app.ports.removeAuth.subscribe(() => {
  localStorage.removeItem('auth')
})
