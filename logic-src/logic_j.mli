(* Auto-generated from "logic.atd" *)
[@@@ocaml.warning "-27-32-35-39"]

type team = Logic_t.team

type id = Logic_t.id

type units_by_team = Logic_t.units_by_team

type units_by_map = Logic_t.units_by_map

type unit_type = Logic_t.unit_type

type unit_ = Logic_t.unit_ = {
  type_: unit_type;
  x: int;
  y: int;
  health: int;
  id: id;
  team: team
}

type unit_list = Logic_t.unit_list

type state = Logic_t.state = {
  turn: int;
  units: unit_list;
  teams: units_by_team;
  map: units_by_map
}

type json = Yojson.Safe.t

type direction = Logic_t.direction

type action_type = Logic_t.action_type

type action = Logic_t.action = { type_: action_type; direction: direction }

type action_list = Logic_t.action_list

type robot_output = Logic_t.robot_output = {
  actions: action_list;
  custom: json
}

type robot_input = Logic_t.robot_input = { state: state; custom: json }

type main_output = Logic_t.main_output = { winner: int; turns: state list }

type main_input = Logic_t.main_input = { p1_code: string }

val write_team :
  Bi_outbuf.t -> team -> unit
  (** Output a JSON value of type {!team}. *)

val string_of_team :
  ?len:int -> team -> string
  (** Serialize a value of type {!team}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_team :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> team
  (** Input JSON data of type {!team}. *)

val team_of_string :
  string -> team
  (** Deserialize JSON data of type {!team}. *)

val write_id :
  Bi_outbuf.t -> id -> unit
  (** Output a JSON value of type {!id}. *)

val string_of_id :
  ?len:int -> id -> string
  (** Serialize a value of type {!id}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_id :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> id
  (** Input JSON data of type {!id}. *)

val id_of_string :
  string -> id
  (** Deserialize JSON data of type {!id}. *)

val write_units_by_team :
  Bi_outbuf.t -> units_by_team -> unit
  (** Output a JSON value of type {!units_by_team}. *)

val string_of_units_by_team :
  ?len:int -> units_by_team -> string
  (** Serialize a value of type {!units_by_team}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_units_by_team :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> units_by_team
  (** Input JSON data of type {!units_by_team}. *)

val units_by_team_of_string :
  string -> units_by_team
  (** Deserialize JSON data of type {!units_by_team}. *)

val write_units_by_map :
  Bi_outbuf.t -> units_by_map -> unit
  (** Output a JSON value of type {!units_by_map}. *)

val string_of_units_by_map :
  ?len:int -> units_by_map -> string
  (** Serialize a value of type {!units_by_map}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_units_by_map :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> units_by_map
  (** Input JSON data of type {!units_by_map}. *)

val units_by_map_of_string :
  string -> units_by_map
  (** Deserialize JSON data of type {!units_by_map}. *)

val write_unit_type :
  Bi_outbuf.t -> unit_type -> unit
  (** Output a JSON value of type {!unit_type}. *)

val string_of_unit_type :
  ?len:int -> unit_type -> string
  (** Serialize a value of type {!unit_type}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_unit_type :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> unit_type
  (** Input JSON data of type {!unit_type}. *)

val unit_type_of_string :
  string -> unit_type
  (** Deserialize JSON data of type {!unit_type}. *)

val write_unit_ :
  Bi_outbuf.t -> unit_ -> unit
  (** Output a JSON value of type {!unit_}. *)

val string_of_unit_ :
  ?len:int -> unit_ -> string
  (** Serialize a value of type {!unit_}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_unit_ :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> unit_
  (** Input JSON data of type {!unit_}. *)

val unit__of_string :
  string -> unit_
  (** Deserialize JSON data of type {!unit_}. *)

val write_unit_list :
  Bi_outbuf.t -> unit_list -> unit
  (** Output a JSON value of type {!unit_list}. *)

val string_of_unit_list :
  ?len:int -> unit_list -> string
  (** Serialize a value of type {!unit_list}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_unit_list :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> unit_list
  (** Input JSON data of type {!unit_list}. *)

val unit_list_of_string :
  string -> unit_list
  (** Deserialize JSON data of type {!unit_list}. *)

val write_state :
  Bi_outbuf.t -> state -> unit
  (** Output a JSON value of type {!state}. *)

val string_of_state :
  ?len:int -> state -> string
  (** Serialize a value of type {!state}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_state :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> state
  (** Input JSON data of type {!state}. *)

val state_of_string :
  string -> state
  (** Deserialize JSON data of type {!state}. *)

val write_json :
  Bi_outbuf.t -> json -> unit
  (** Output a JSON value of type {!json}. *)

val string_of_json :
  ?len:int -> json -> string
  (** Serialize a value of type {!json}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_json :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> json
  (** Input JSON data of type {!json}. *)

val json_of_string :
  string -> json
  (** Deserialize JSON data of type {!json}. *)

val write_direction :
  Bi_outbuf.t -> direction -> unit
  (** Output a JSON value of type {!direction}. *)

val string_of_direction :
  ?len:int -> direction -> string
  (** Serialize a value of type {!direction}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_direction :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> direction
  (** Input JSON data of type {!direction}. *)

val direction_of_string :
  string -> direction
  (** Deserialize JSON data of type {!direction}. *)

val write_action_type :
  Bi_outbuf.t -> action_type -> unit
  (** Output a JSON value of type {!action_type}. *)

val string_of_action_type :
  ?len:int -> action_type -> string
  (** Serialize a value of type {!action_type}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_action_type :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> action_type
  (** Input JSON data of type {!action_type}. *)

val action_type_of_string :
  string -> action_type
  (** Deserialize JSON data of type {!action_type}. *)

val write_action :
  Bi_outbuf.t -> action -> unit
  (** Output a JSON value of type {!action}. *)

val string_of_action :
  ?len:int -> action -> string
  (** Serialize a value of type {!action}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_action :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> action
  (** Input JSON data of type {!action}. *)

val action_of_string :
  string -> action
  (** Deserialize JSON data of type {!action}. *)

val write_action_list :
  Bi_outbuf.t -> action_list -> unit
  (** Output a JSON value of type {!action_list}. *)

val string_of_action_list :
  ?len:int -> action_list -> string
  (** Serialize a value of type {!action_list}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_action_list :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> action_list
  (** Input JSON data of type {!action_list}. *)

val action_list_of_string :
  string -> action_list
  (** Deserialize JSON data of type {!action_list}. *)

val write_robot_output :
  Bi_outbuf.t -> robot_output -> unit
  (** Output a JSON value of type {!robot_output}. *)

val string_of_robot_output :
  ?len:int -> robot_output -> string
  (** Serialize a value of type {!robot_output}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_robot_output :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> robot_output
  (** Input JSON data of type {!robot_output}. *)

val robot_output_of_string :
  string -> robot_output
  (** Deserialize JSON data of type {!robot_output}. *)

val write_robot_input :
  Bi_outbuf.t -> robot_input -> unit
  (** Output a JSON value of type {!robot_input}. *)

val string_of_robot_input :
  ?len:int -> robot_input -> string
  (** Serialize a value of type {!robot_input}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_robot_input :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> robot_input
  (** Input JSON data of type {!robot_input}. *)

val robot_input_of_string :
  string -> robot_input
  (** Deserialize JSON data of type {!robot_input}. *)

val write_main_output :
  Bi_outbuf.t -> main_output -> unit
  (** Output a JSON value of type {!main_output}. *)

val string_of_main_output :
  ?len:int -> main_output -> string
  (** Serialize a value of type {!main_output}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_main_output :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> main_output
  (** Input JSON data of type {!main_output}. *)

val main_output_of_string :
  string -> main_output
  (** Deserialize JSON data of type {!main_output}. *)

val write_main_input :
  Bi_outbuf.t -> main_input -> unit
  (** Output a JSON value of type {!main_input}. *)

val string_of_main_input :
  ?len:int -> main_input -> string
  (** Serialize a value of type {!main_input}
      into a JSON string.
      @param len specifies the initial length
                 of the buffer used internally.
                 Default: 1024. *)

val read_main_input :
  Yojson.Safe.lexer_state -> Lexing.lexbuf -> main_input
  (** Input JSON data of type {!main_input}. *)

val main_input_of_string :
  string -> main_input
  (** Deserialize JSON data of type {!main_input}. *)

