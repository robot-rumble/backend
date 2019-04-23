open Base
open Logic_t
open Lwt.Infix

let letters = String.to_array "abcdefghijklmnopqrstuvwxyz"

let generate_id () =
  String.init 5 ~f:(fun _ -> Array.random_element_exn letters)

let create_unit type_ x y health team =
  {Logic_t.type_; coords= (x, y); health; team; id= generate_id ()}

let create_rect_map side =
  Array.init side ~f:(fun x ->
      Array.init side ~f:(fun y ->
          if x = 0 || x = side - 1 || y = 0 || y = side - 1 then Wall
          else Empty ) )

let create_circle_map radius =
  let is_wall x y = ((x - radius) ** 2) + ((y - radius) ** 2) > radius ** 2 in
  Array.init (radius * 2) ~f:(fun x ->
      Array.init (radius * 2) ~f:(fun y -> if is_wall x y then Wall else Empty)
  )

let rec random_loc map =
  let x = Random.int @@ Array.length map
  and y = Random.int @@ Array.length map.(0) in
  match map.(x).(y) with Empty -> (x, y) | _ -> random_loc map

let create_id_list unit_list =
  List.map unit_list ~f:(fun unit_ -> (unit_.id, unit_))

let create_team_list unit_list =
  let red, blue =
    List.partition_map unit_list ~f:(fun (_, unit) ->
        match unit.team with Red -> `Fst unit.id | Blue -> `Snd unit.id )
  in
  [("red", red); ("blue", blue)]

let radius' = 10
let unit_num' = 6
let other_team = function Red -> Blue | Blue -> Red

let lwt_join = function
  | [a; b] -> Lwt.both a b >|= fun (a, b) -> [a; b]
  | _ -> failwith "Join requires two arguments."

let check_actions team actions units =
  List.iter actions ~f:(fun (id, _action) ->
      match List.Assoc.find ~equal:String.( = ) units id with
      | Some unit_ ->
          if Poly.(unit_.team <> team) then
            failwith "Action ID belongs to opposing team."
          else ()
      | None -> failwith "Action ID does not exist." )

let compute_coords (x, y) direction =
  match direction with
  | Left -> (x - 1, y)
  | Right -> (x + 1, y)
  | Up -> (x, y - 1)
  | Down -> (x, y + 1)

let compare_coords (x1, y1) (x2, y2) = x1 - x2 + (y1 - y2)
let coords_equal (x1, y1) (x2, y2) = x1 = x2 && y1 = y2

let determine_winner state =
  let res =
    List.max_elt state.teams ~compare:(fun (_, units1) (_, units2) ->
        List.length units1 - List.length units2 )
  in
  match res with Some (team, _) -> team | None -> assert false

let rec validate_movement_map movement_map map units =
  let movement_map, conflicting_moves =
    List.partition_map movement_map ~f:(fun (id, coords) ->
        match map.(fst coords).(snd coords) with
        | Empty -> `Fst (id, coords)
        | _ -> `Snd (id, coords) )
  in
  List.iter conflicting_moves ~f:(fun (id, _) ->
      let coords = (Caml.List.assoc id units).coords in
      map.(fst coords).(snd coords) <- Unit id );
  if List.is_empty conflicting_moves then movement_map
  else validate_movement_map movement_map map units

let rec run_turn run turn units map state_list =
  let team_list = create_team_list units in
  let state = {turn= turn + 1; units; teams= team_list; map} in
  let state_list = state :: state_list in
  if turn = 9 then Lwt.return state_list
  else
    let inputs =
      List.map [Red; Blue] ~f:(fun team ->
          {state; friend= team; foe= other_team team} )
    in
    inputs
    |> List.map ~f:Logic_j.string_of_robot_input
    |> List.map ~f:run |> lwt_join
    >>= fun (result : string list) ->
    let output_list = List.map result ~f:Logic_j.robot_output_of_string in
    let team_output_list = List.zip_exn [Red; Blue] output_list in
    List.iter team_output_list ~f:(fun (team, output) ->
        check_actions team output.actions units );
    let all_actions =
      List.concat_map output_list ~f:(fun output -> output.actions)
    in
    let movement_map =
      List.filter_map all_actions ~f:(fun (id, action) ->
          match action.type_ with
          | Move ->
              Some
                ( id
                , compute_coords (Caml.List.assoc id units).coords
                    action.direction )
          | _ -> None )
    in
    let conflicting_moves =
      List.find_all_dups movement_map
        ~compare:(fun (_, coords1) (_, coords2) ->
          compare_coords coords1 coords2 )
    in
    let movement_map =
      List.filter movement_map ~f:(fun (_, coords) ->
          not
          @@ List.Assoc.mem
               (List.Assoc.inverse conflicting_moves)
               ~equal:coords_equal coords )
    in
    List.iter movement_map ~f:(fun (id, _) ->
        let coords = (Caml.List.assoc id units).coords in
        map.(fst coords).(snd coords) <- Empty );
    let movement_map = validate_movement_map movement_map map units in
    List.iter movement_map ~f:(fun (id, coords) ->
        map.(fst coords).(snd coords) <- Unit id );
    let units =
      List.Assoc.map units ~f:(fun unit_ ->
          match Caml.List.assoc_opt unit_.id movement_map with
          | Some coords -> {unit_ with coords}
          | None -> unit_ )
    in
    run_turn run (turn + 1) units map state_list

let start run =
  Random.self_init ();
  let map = create_rect_map radius' in
  let units =
    List.init unit_num' ~f:(fun i ->
        let x, y = random_loc map and team = if i % 2 = 0 then Red else Blue in
        let unit = create_unit Soldier x y 10 team in
        (unit.id, unit) )
  in
  List.iter units ~f:(fun (_, unit_) ->
      map.(fst unit_.coords).(snd unit_.coords) <- Unit unit_.id );
  run_turn run 0 units map []
  >|= fun states ->
  Logic_j.string_of_main_output
    {turns= states |> List.rev; winner= determine_winner @@ List.hd_exn states}
