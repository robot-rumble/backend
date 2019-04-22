import { Elm } from './Main.elm'
import '../css/app.scss'

import { main } from '../logic-src/_build/default/web.js'
import Realm from 'ecma-proposal-realms'
let realm = Realm.makeRootRealm()
realm.global.console = console

let app = Elm.Main.init({
  node: document.getElementById('root'),
  windowWidth: window.innerWidth,
})

setTimeout(() => app.ports.getOutput.send(3), 1000)

app.ports.startEval.subscribe((code) => {
  let time = Date.now()

  // let result = main({ realm, p1: code }, (result) => {
  //   console.log('=========FINAL=========')
  //   console.log(JSON.parse(result))
  //   console.log(`Time taken: ${(Date.now() - time) / 1000}`)

  //   app.ports.getOutput.send(JSON.parse(result))
  // })
})
