import { Elm } from './Main.elm'
import '../css/app.scss'

import { main } from '../logic-src/_build/default/web'
import SES from 'ses'
import { initCodeMirror } from './codemirror'

const realm = SES.makeSESRootRealm({ consoleMode: 'allow', errorStackMode: 'allow' })

let app = Elm.Main.init({
  node: document.getElementById('root'),
  windowWidth: window.innerWidth,
})

initCodeMirror(app.ports.changeCode.send)

app.ports.startEval.subscribe((code) => {
  let time = Date.now()
  try {
    let result = main({ realm, p1: code }, (result) => {
      console.log('=========FINAL=========')
      console.log(`Time taken: ${(Date.now() - time) / 1000}`)
      app.ports.getOutput.send(JSON.parse(result))
    })
  } catch (e) {
    console.log('Root Error!')
    console.log(e.message)
    console.error(new Error(e))
  }
})
