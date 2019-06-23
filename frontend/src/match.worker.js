import stdlib from './stdlib.raw.py'
import { main as runLogic } from 'logic'
import _ from 'lodash'

let rpPromise = import('rustpython_wasm')

// to fix some weird bug
self.Window = self.constructor

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

      let vms = ['robot1', 'robot2'].map((name) => {
        rp.vmStore.destroy(name)
        return rp.vmStore.init(name, false)
      })

      let codes = [data.code1, data.code2].map((code) => code + '\n' + stdlib)

      _.zip(codes, vms).forEach(([code, vm]) => {
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
      })

      const [run1, run2] = vms.map((vm) => {
        const main = (args) => vm.eval('main')([args])
        return (args) => {
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
      })
      const turnCallback = (turn) => self.postMessage({ type: 'getProgress', data: turn })

      runLogic({ run1, run2, turnNum: data.turnNum, turnCallback }, (output) => {
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
