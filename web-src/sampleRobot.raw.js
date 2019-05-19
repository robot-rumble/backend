function main(input) {
  let actions = {}
  console.log(input)
  console.log(displayMap(input.state.objs, input.state.map))

  for (let id of input.state.teams[input.team]) {
    actions[id] = {
      type_: input.state.turn % 2 == 0 ? 'Move' : 'Attack',
      direction: input.team == 'red' ? 'Right' : 'Down',
    }
  }

  return { actions }
}
