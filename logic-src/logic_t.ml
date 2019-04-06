(* Auto-generated from "logic.atd" *)
              [@@@ocaml.warning "-27-32-35-39"]

type team = string

type id = string

type units_by_team = (team * id list) list

type units_by_map = id option list list

type unit_type = [ `Soldier ]

type unit_ = {
  type_: unit_type;
  x: int;
  y: int;
  health: int;
  id: id;
  team: team
}

type unit_list = (id * unit_) list

type state = {
  turn: int;
  units: unit_list;
  teams: units_by_team;
  map: units_by_map
}

type json = Yojson.Safe.t

type direction = [ `Left | `Right | `Up | `Down ]

type action_type = [ `Move | `Attack ]

type action = { type_: action_type; direction: direction }

type action_list = (id * action) list

type robot_output = { actions: action_list; custom: json }

type robot_input = { state: state; custom: json }

type main_output = { winner: int; turns: state list }

type main_input = { p1_code: string }
