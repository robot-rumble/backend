open Base
open Logic_t

let letters = String.to_array "abcdefghijklmnopqrstuvwxyz"

let generate_id () =
  String.init 5 ~f:(fun _ -> Array.random_element_exn letters)

let create_unit type_ x y health team =
  {Logic_t.type_; x; y; health; team; id= generate_id ()}

let create_circle_map radius =
  let is_wall x y = ((x - radius) ** 2) + ((y - radius) ** 2) > radius ** 2 in
  Array.init radius ~f:(fun x ->
      Array.init radius ~f:(fun y -> if is_wall x y then Wall else Empty) )

let rec random_loc map =
  let x = Random.int @@ Array.length map
  and y = Random.int @@ Array.length map.(0) in
  match map.(x).(y) with Empty -> (x, y) | _ -> random_loc map

let create_id_list unit_list =
  List.map unit_list ~f:(fun unit_ -> (unit_.id, unit_))

let create_team_list unit_list =
  let red, blue =
    List.partition_map unit_list ~f:(fun unit ->
        match unit.team with Red -> `Fst unit.id | Blue -> `Snd unit.id )
  in
  [("red", red); ("blue", blue)]

let radius' = 10
let unit_num' = 6

let start run input =
  let initial_map = create_circle_map radius' in
  let initial_units =
    List.init unit_num' ~f:(fun i ->
        let x, y = random_loc initial_map
        and team = if i % 2 = 0 then Red else Blue in
        create_unit Soldier x y 10 team )
  in
  List.iter initial_units ~f:(fun unit ->
      initial_map.(unit.x).(unit.y) <- Unit unit.id );
  let id_list = create_id_list initial_units
  and team_list = create_team_list initial_units in
  let state = {turn= 0; units= id_list; teams= team_list; map= initial_map} in
  let result = run input.p1_code (Logic_j.string_of_state state) in
  result
