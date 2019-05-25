import { main } from 'logic'

let rpPromise = import('rustpython_wasm')

let errorToObj = (e) => ({
  message: e.message,
  col: e.col,
  row: e.row,
})

self.addEventListener('message', ({ data: code }) => {
  rpPromise
    .then((rp) => {
      let time = Date.now()

      let language = 'python'
      let turnNum = 10

      let func
      if (language === 'python') {
        rp.vmStore.destroy('robot')
        const vm = rp.vmStore.init('robot', false)

        vm.addToScope('print', (val) => console.log(val))

        try {
          vm.exec(code)
        } catch (e) {
          self.postMessage({
            type: 'getOutput',
            data: errorToObj(e),
          })
          return
        }

        func = (args) => vm.eval('main')([args])
      } else if (language === 'js') {
        func = realm.eval(`(args) => {
        ${code};
        main(args)
      }`)
      }

      const run = (args) => {
        args = JSON.parse(args)
        try {
          return JSON.stringify(func(args, {}))
        } catch (e) {
          self.postMessage({ type: 'robotError', data: errorToObj(e) })
        }
      }

      let callback = (turn) => self.postMessage({ type: 'getProgress', data: turn })

      main({ run, turnNum, turnCallback: callback }, (result) => {
        console.log('=========FINAL=========')
        console.log(`Time taken: ${(Date.now() - time) / 1000}`)
        self.postMessage({ type: 'getOutput', data: JSON.parse(result) })
      })
    })
    .catch((e) => {
      self.postMessage({ type: 'error', data: errorToObj(e) })
    })
})
