import { Elm } from './Main.elm'
import '../css/app.scss'

import { main } from '../logic-src/_build/default/web.js'
import Realm from 'ecma-proposal-realms'
let realm = Realm.makeRootRealm()
realm.global.console = console
realm.global.displayMap = (map) =>
  map
    .map((col) =>
      col
        .map((unit) => {
          if (typeof unit === 'object') {
            return unit[1].slice(0, 5)
          } else {
            if (unit === 'Empty') return '     '
            else if (unit === 'Wall') return 'OOOOO'
          }
        })
        .join(' '),
    )
    .join('\n')

let app = Elm.Main.init({
  node: document.getElementById('root'),
  windowWidth: window.innerWidth,
})

app.ports.startEval.subscribe((code) => {
  let time = Date.now()

  try {
    let result = main({ realm, p1: code }, (result) => {
      console.log('=========FINAL=========')
      console.log(`Time taken: ${(Date.now() - time) / 1000}`)
      app.ports.getOutput.send(JSON.parse(result))
    })
  } catch (e) {
    console.log('OCaml Error: ', e)
  }
})
