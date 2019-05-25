import { main as runLogic } from 'logic'

let rpPromise = import('rustpython_wasm')

let errorToObj = (e) => ({
  message: e.message,
  // elm expects a null value for missing field
  col: e.col || null,
  row: e.row || null,
})

self.addEventListener('message', ({ data: { code, turnNum } }) => {
  rpPromise
    .then((rp) => {
      const time = Date.now()

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

      const main = (args) => vm.eval('main')([args])
      const run = (args) => {
        args = JSON.parse(args)
        try {
          return JSON.stringify(main(args, {}))
        } catch (e) {
          self.postMessage({
            type: 'getOutput',
            data: errorToObj(e),
          })
        }
      }
      const turnCallback = (turn) => self.postMessage({ type: 'getProgress', data: turn })

      runLogic({ run, turnNum, turnCallback }, (output) => {
        console.log('=========FINAL=========')
        console.log(`Time taken: ${(Date.now() - time) / 1000}`)
        self.postMessage({ type: 'getOutput', data: JSON.parse(output) })
      })
    })
    .catch((e) => {
      self.postMessage({ type: 'error', data: errorToObj(e) })
    })
})
