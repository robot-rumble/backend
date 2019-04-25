open Base
open Logic_t
open Lwt.Infix

let team_names = ["red"; "blue"]
let ( >> ) f g x = f (g x)
let letters = String.to_array "abcdefghijklmnopqrstuvwxyz"
let id_length = 5

let generate_id () =
  String.init id_length ~f:(fun _ -> Array.random_element_exn letters)

let create_basic_obj coords = {coords; id= generate_id ()}
let create_terrain type_ coords = (create_basic_obj coords, Terrain {type_})
let unit_health = 10

let create_unit type_ coords team =
  (create_basic_obj coords, Unit {type_; health= unit_health; team})

module Coords = struct
  module T = struct type t = int * int [@@deriving compare, sexp_of] end
  include T
  include Comparator.Make (T)

  let equal (x1, y1) (x2, y2) = x1 = x2 && y1 = y2
end

module Map = struct
  include Map

  let update_exn map key ~f =
    Map.update map key ~f:(function Some v -> f v | None -> assert false)

  let append_exn map1 map2 =
    match Map.append ~lower_part:map1 ~upper_part:map2 with
    | `Ok res -> res
    | `Overlapping_key_ranges -> assert false

  let partition_tf_keys map ~f =
    Map.partitioni_tf map ~f:(fun ~key ~data:_ -> f key)
end

let create_grid size = List.zip_exn (List.range 0 size) (List.range 0 size)

let filter_empty type_ size =
  List.filter ~f:(fun (x, y) ->
      match type_ with
      | Circle ->
          let radius = size / 2 in
          let is_wall x y =
            ((x - radius) ** 2) + ((y - radius) ** 2) > radius ** 2
          in
          is_wall x y
      | Rect -> x = 0 || x = size - 1 || y = 0 || y = size - 1 )

let create_map_vars map =
  let terrains = List.map map ~f:(create_terrain Wall) in
  let map =
    List.map terrains ~f:(fun (base, _terrain) -> (base.coords, base.id))
    |> Map.of_alist_exn (module Coords)
  in
  (terrains, map)

let create_map type_ size =
  create_grid size |> filter_empty type_ size |> create_map_vars

let rec random_loc map size =
  let x = Random.int size and y = Random.int size in
  match Map.find map (x, y) with
  | None -> (x, y)
  | Some _ -> random_loc map size

let create_teams objs team_names =
  let init =
    List.map team_names ~f:(fun team -> (team, []))
    |> Map.of_alist_exn (module String)
  in
  List.fold (Map.data objs) ~init ~f:(fun acc (_base, details) ->
      match details with
      | Unit unit_ -> Map.add_multi acc ~key:unit_.team ~data:_base.id
      | Terrain _ -> acc )
  |> Map.to_alist

let create_array_map map size =
  Array.init size ~f:(fun x ->
      Array.init size ~f:(fun y -> Map.find map (x, y)) )

let lwt_join = function
  | [a; b] -> Lwt.both a b >|= fun (a, b) -> [a; b]
  | _ -> failwith "Join requires two arguments."

let check_actions team actions objs =
  List.iter actions ~f:(fun (id, _action) ->
      match Map.find objs id with
      | Some (_base, Unit unit_) ->
          if String.(unit_.team <> team) then
            failwith "Action ID belongs to opposing team."
          else ()
      | Some (_base, Terrain _) -> failwith "Action ID belongs to terrain"
      | None -> failwith "Action ID does not exist." )

let compute_coords (x, y) direction =
  match direction with
  | Left -> (x - 1, y)
  | Right -> (x + 1, y)
  | Up -> (x, y - 1)
  | Down -> (x, y + 1)

let determine_winner (state : turn_state) =
  let teams =
    create_teams (Map.of_alist_exn (module String) state.objs) team_names
  in
  let res =
    List.max_elt teams ~compare:(fun (_, units1) (_, units2) ->
        List.length units1 - List.length units2 )
  in
  match res with Some (team, _) -> team | None -> assert false

let get_obj_coords objs id =
  let base, _ = Map.find_exn objs id in
  base.coords

let rec validate_movement_map movement_map map objs =
  let conflicting_moves, movement_map =
    Map.partition_tf_keys movement_map ~f:(Map.mem map)
  in
  let map =
    Map.fold conflicting_moves ~init:map ~f:(fun ~key:_coords ~data:id map ->
        Map.set map ~key:(get_obj_coords objs id) ~data:id )
  in
  if Map.is_empty conflicting_moves then (movement_map, map)
  else validate_movement_map movement_map map objs

let map_size = 10
let team_unit_num = 6

let rec run_turn run turn objs (map : (Coords.t, id, 'a) Map.t) state_list =
  let state = {turn= turn + 1; objs= Map.to_alist objs} in
  let state_list = state :: state_list in
  if turn = 9 then Lwt.return state_list
  else
    let input_teams = create_teams objs team_names in
    let input_map = create_array_map map map_size in
    let input_state =
      {turn= state.turn; objs= state.objs; teams= input_teams; map= input_map}
    in
    let inputs =
      List.map team_names ~f:(fun team -> {team; state= input_state})
    in
    inputs
    |> List.map ~f:Logic_j.string_of_robot_input
    |> List.map ~f:run |> lwt_join
    >>= fun (result : string list) ->
    let output_list = List.map result ~f:Logic_j.robot_output_of_string in
    let team_output_list = List.zip_exn team_names output_list in
    List.iter team_output_list ~f:(fun (team, output) ->
        check_actions team output.actions objs );
    let all_actions =
      List.concat_map output_list ~f:(fun output -> output.actions)
    in
    let movement_map =
      List.filter_map all_actions ~f:(fun (id, action) ->
          match action.type_ with
          | Move ->
              Some
                (compute_coords (get_obj_coords objs id) action.direction, id)
          | _ -> None )
      |> Map.of_alist_exn (module Coords)
    in
    let conflicting_moves =
      List.find_all_dups (Map.keys movement_map)
        ~compare:Coords.comparator.compare
    in
    let movement_map =
      Map.filter_keys movement_map
        ~f:(List.mem conflicting_moves ~equal:Coords.equal)
    in
    let map =
      Map.filter map ~f:(fun id ->
          not @@ Map.mem movement_map (get_obj_coords objs id) )
    in
    let movement_map, map = validate_movement_map movement_map map objs in
    let map = Map.append_exn map movement_map in
    let objs =
      Map.fold map ~init:objs ~f:(fun ~key:coords ~data:id objs ->
          Map.update_exn objs id ~f:(fun (base, details) ->
              ({base with coords}, details) ) )
    in
    run_turn run (turn + 1) objs map state_list

let start run =
  Random.self_init ();
  let terrains, map = create_map Rect map_size in
  let units =
    List.concat_map team_names ~f:(fun team ->
        List.init team_unit_num ~f:(fun _ ->
            let coords = random_loc map map_size in
            create_unit Soldier coords team ) )
  in
  let objs =
    List.append units terrains
    |> List.map ~f:(fun ((base, _) as obj) -> (base.id, obj))
    |> Map.of_alist_exn (module String)
  in
  run_turn run 0 objs map []
  >|= fun states ->
  {turns= List.rev states; winner= determine_winner @@ List.hd_exn states}
  |> Logic_j.string_of_main_output
