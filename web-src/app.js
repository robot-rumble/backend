import { Elm } from './Main.elm'
import '../css/app.scss'

import { main } from '../logic-src/_build/default/web.js'
import Realm from 'ecma-proposal-realms'
let realm = Realm.makeRootRealm()
realm.global.console = console

let code = `
function main (input) {
  console.log(input)
  return { actions: {}, custom: {} }
}
`

let time = Date.now()
let result = main({ realm, p1: code }, (result) => {
  console.log('=========FINAL=========')
  console.log(JSON.parse(result))
  console.log(`Time taken: ${(Date.now() - time) / 1000}`)
})

// --------------------------------

// import Realm from 'ecma-proposal-realms'
// let realm = Realm.makeRootRealm()

// let main

// main = realm.evaluate('let i = 0; let main = () => {i++; return i}; main')
// console.log(main())

// setTimeout(() => console.log(main()), 1000)

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
