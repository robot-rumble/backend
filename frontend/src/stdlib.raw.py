def check_enum(val, options):
  if not val in options:
    raise Exception(f'"{val}" must be one of {options}')

def direction_action(func):
  def wrapper(direction):
    global current_action
    direction = direction.capitalize()
    check_enum(direction, ['Left', 'Right', 'Up', 'Down'])
    current_action = {
      'type_': func.__name__.capitalize(),
      'direction': direction
    }
  return wrapper

@direction_action
def move():
  pass

@direction_action
def attack():
  pass


def main(main_input):
  global obj_by_id, objs_by_team, ids_by_team, obj_by_loc, id_by_loc, move, attack

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


  global current_action
  actions = {}

  for id in state['teams'][team]:
    current_action = None
    robot(state['turn'], state['objs'][id])
    if not current_action:
      raise Exception('Robot did not call any actions!')
    actions[id] = current_action

  return { 'actions': actions }

