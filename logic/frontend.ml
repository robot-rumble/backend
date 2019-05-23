open Js_of_ocaml
open Lwt.Infix

class type input =
  object
    method run : string -> Js.js_string Js.t Js.meth

    method turn_num : int Js.prop
  end

let input_var = "main_input"

let main (input : input Js.t) callback =
  let run (robot_input : string) =
    Lwt.wrap (fun () -> input##run robot_input |> Js.to_string)
  in
  Logic.start run 29 >|= Js.string >|= callback

let _ = Js.export "main" main
