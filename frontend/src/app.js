import { Elm } from './Main.elm'
import './css/app.scss'

import './codemirror'

window.turnNum = 10
window.language = 'python'
window.runCount = 0

class Game extends HTMLElement {
  connectedCallback() {
    const app = Elm.Main.init({
      node: this,
      flags: {
        totalTurns: window.turnNum,
        auth,
        endpoint:
          process.env.NODE_ENV === 'production'
            ? 'https://robotrumble.org/api/v1'
            : 'http://localhost:4000/api/v1',
      },
    })

    const matchWorker = new Worker('/worker.js')

    app.ports.startEval.subscribe(([code1, code2, turnNum]) => {
      window.runCount++
      matchWorker.postMessage({ code1, code2, turnNum })
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
  }
}

customElements.define(Game)
