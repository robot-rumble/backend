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
    List.partition_map unit_list ~f:(fun (_, unit) ->
        match unit.team with Red -> `Fst unit.id | Blue -> `Snd unit.id )
  in
  [("red", red); ("blue", blue)]

let radius' = 10
let unit_num' = 6
let other_team = function Red -> Blue | Blue -> Red

let run_turn run turn units map custom_fields =
  let team_list = create_team_list units in
  let state = {turn= turn + 1; units; teams= team_list; map} in
  let inputs =
    List.map [Red; Blue] ~f:(fun team ->
        { state
        ; custom= Caml.List.assoc team custom_fields
        ; friend= team
        ; foe= other_team team } )
  in
  inputs |> List.map ~f:run |> Lwt.join
  >|= fun (result : string) ->
  let output_list = List.map result ~f:Logic_j.robot_output_of_string in
  let output_list = List.zip_exn [Red; Blue] output_list in
  List.iter output_list ~f:(fun (team, actions) ->
      List.iter actions ~f:(fun (id, action) ->
          match List.Assoc.find ~equal:String.( = ) units id with
          | Some unit_ ->
              if Poly.(unit_.team = team) then
                failwith "Action ID belongs to opposing team."
              else ()
          | None -> failwith "Action ID does not exist." ) )

let start run =
  let map = create_circle_map radius' in
  let units =
    List.init unit_num' ~f:(fun i ->
        let x, y = random_loc map and team = if i % 2 = 0 then Red else Blue in
        let unit = create_unit Soldier x y 10 team in
        (unit.id, unit) )
  in
  List.iter units ~f:(fun (_, unit) -> map.(unit.x).(unit.y) <- Unit unit.id);
  let custom_fields = [(Red, ""); (Blue, "")] in
  run_turn run 0 units map custom_fields
