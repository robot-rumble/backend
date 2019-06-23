def _check_enum(val, options):
  if not val in options:
    options_string = ' / '.join(options)
    raise Exception(f'"{val}" must be one of {options_string}')

def _direction_action(name):
  def wrapper(direction):
    direction = direction.capitalize()
    _check_enum(direction, ['Left', 'Right', 'Up', 'Down'])
    return {
      'type_': name.capitalize(),
      'direction': direction
    }
  return wrapper

move = _direction_action("move")
attack = _direction_action("attack")

def main(main_input):
  global obj_by_id, objs_by_team, ids_by_team
  global obj_by_loc, id_by_loc, move, attack, other_team

  state = main_input['state']
  team = main_input['team']

  def obj_by_id(id):
    return state['objs'][id]

  def objs_by_team(team):
    return [obj_by_id(id) for id in ids_by_team(team)]
  def ids_by_team(team):
    return state['teams'][team]

  def obj_by_loc(x, y):
    return obj_by_id(id_by_loc(x, y))
  def id_by_loc(x, y):
    return state['map'][x][y]

  def other_team():
    if team == "red":
      return "blue"
    elif team == "blue":
      return "red"
    else:
      raise Exception("team is neither red nor blue")

  global current_action
  actions = {}

  for id in state['teams'][team]:
    try:
      robot
    except NameError:
      raise Exception('You must define a "robot" function.')
    if robot.__code__.co_argcount != 2:
      raise Exception('Your "robot" function must accept two values: the current turn and robot details.')
    current_action = robot(state['turn'], state['objs'][id])
    if not current_action:
      raise Exception('Robot did not return an action!')
    actions[id] = current_action

  return { 'actions': actions }

