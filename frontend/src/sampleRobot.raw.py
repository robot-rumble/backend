def main(inp):
  actions = {}
  for id in inp['state']['teams'][inp['team']]:
    actions[id] = {
      'direction': 'Down'
    }
    if inp['state']['turn'] % 2 == 0:
      actions[id]['type_'] = 'Move'
      actions[id]['direction'] = 'Right'
    else:
      actions[id]['type_'] = 'Attack'
      actions[id]['direction'] = 'Down'

  return { 'actions': actions }
