open Base

let letters = String.to_array "abcdefghijklmnopqrstuvwxyz"

let generate_id () =
  String.init 5 ~f:(fun _ -> Array.random_element_exn letters)

(* let create_unit ~type_ ~x ~y ~health ~team = *)
(*   {type_; x; y; health; team; id= generate_id ()} *)

(* let create_id_map unit_list = *)
(*   List.map unit_list ~f:(fun unit_ -> (unit_.id, unit_)) *)
(*   |> Map.of_alist_exn (module String) *)

(* let start run input = *)
(*   let initial_state = {p1= [create_unit `Soldier 0 0 10]; p2= []} in *)
(*   let result = run input initial_state in *)
(*   result *)

let start () = "asf"
