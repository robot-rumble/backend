import { Elm } from './Main.elm'
import '../css/app.scss'

// import { main } from '../lib/js/logic-src/Main.bs'
// main('asdf')
//
import { main } from '../logic-src/_build/default/web.js'
import Realm from 'ecma-proposal-realms'
let r = Realm.makeRootRealm()

console.log(main({ p1: '(state => state.soldier)(state)', realm: r }))

// let app = Elm.Main.init({
//   node: document.getElementById('root'),
//   windowWidth: window.innerWidth,
// })

// app.ports.startEval.subscribe((code) => {
//   let state = {1: {}, 2: {}}
//   for (let i = 0; i++; i < 100) {
//     {moves, p1state} = r.evaluate(code)

//   }
//   let round = 0
//   let output = [{ class: 'soldier', x: 0, y: 0, health: 10 }]

//   app.ports.getOutput.send(output)
// })

/*
{
  1: {
    '7ovzxb': {class: "soldier", health: 10},
    '9sjskc': {class: "soldier", health: 10},
  },
  2: {
    'ybdp3p': {class: "base", health: 100},
  }
}


{
  friend: {
    '7ovzxb': {class: "soldier", health: 10},
    '9sjskc': {class: "soldier", health: 10},
  },
  foe: {
    'ybdp3p': {class: "base", health: 100}
  }
}

{
  '7ovzxb': {type: "move", direction: "up"}
}



 */
