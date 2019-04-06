open Js_of_ocaml

class type realm =
  object
    method global : Js.Unsafe.any Js.t Js.prop

    method evaluate : string -> string Js.meth
  end

class type input =
  object
    method p1 : string Js.readonly_prop

    method realm : realm Js.t Js.readonly_prop
  end

let main () = Logic.start ()

(* let run code state = *)
(*   Js.Unsafe.set input##.realm##.global "state" state; *)
(*   let result = input##.realm##evaluate code in *)
(*   Logic_j.robot_output_of_string result *)
(* in *)
(* Main.start run {p1_code= input##.p1} *)

let _ = Js.export "main" main
