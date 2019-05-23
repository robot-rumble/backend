import { Elm } from './Main.elm'
import './css/app.scss'

import { main } from 'logic'
import SES from 'ses'
import './codemirror'

const realm = SES.makeSESRootRealm({ consoleMode: 'allow', errorStackMode: 'allow' })

let rpPromise = import('rustpython_wasm')

let app = Elm.Main.init({
  node: document.getElementById('root'),
  windowWidth: window.innerWidth,
})

app.ports.startEval.subscribe((code) => {
  rpPromise.then((rp) => {
    let time = Date.now()

    rp.vmStore.destroy('robot')
    const vm = rp.vmStore.init('robot', false)

    vm.setStdout()

    try {
      vm.exec(code)
    } catch (err) {
      console.error(err)
    }

    const func = vm.eval('main')
    const run = (args) => {
      try {
        return JSON.stringify(func([JSON.parse(args)], {}))
      } catch (e) {
        console.log('Inside Error!')
        console.log(e.message)
        console.error(new Error(e))
      }
    }

    try {
      let result = main({ run }, (result) => {
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
})
