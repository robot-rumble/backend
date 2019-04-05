type input = {
  p1_code : string;
}

type output = {
  winner : int;
  turns : turn list;
}

and turn = {
  p1 : unit_ list;
  p2 : unit_ list;
}

and unit_ = {
  id : string;
  type_ : unit_type;
  x : int;
  y : int;
  health : int;
  next_action : action;
}

and unit_type = Soldier

and action = {
  type_ : action_type;
  direction: direction;
}

and action_type = Move | Attack

and direction = Left | Right | Up | Down

let start run input =
  run input
