import stdlib from './stdlib.raw.py'
import { main as runLogic } from 'logic'

let rpPromise = import('rustpython_wasm')

let errorToObj = (e) => {
  // elm expects a null value for missing field
  let errorLoc = null
  if (e.row && e.col && e.endrow && e.endcol) {
    errorLoc = {
      line: e.row,
      ch: e.col,
      endline: e.endrow,
      endch: e.endcol,
    }
  }
  return {
    message: e.message,
    errorLoc,
  }
}

self.addEventListener('message', ({ data }) => {
  rpPromise
    .then((rp) => {
      const startTime = Date.now()

      rp.vmStore.destroy('robot')
      const vm = rp.vmStore.init('robot', false)

      vm.addToScope('print', (val) => console.log(val))

      let code = data.code + '\n' + stdlib

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

      runLogic({ run, turnNum: data.turnNum, turnCallback }, (output) => {
        console.log(`Time taken: ${(Date.now() - startTime) / 1000}s`)
        self.postMessage({ type: 'getOutput', data: JSON.parse(output) })
      })
    })
    .catch((e) => {
      let data
      if (e.message) {
        // JS error
        data = errorToObj(e)
      } else {
        // OCAML error
        data = e
      }
      self.postMessage({ type: 'error', data })
    })
})
