open Js_of_ocaml
open Lwt.Infix

class type realm =
  object
    method global : Js.Unsafe.any Js.t Js.prop

    method evaluate : string -> Js.js_string Js.t Js.meth
  end

class type input =
  object
    method p1_code : Js.js_string Js.t Js.readonly_prop

    method realm : realm Js.t Js.readonly_prop
  end

let input_var = "main_input"

let main (input : input Js.t) callback =
  let code =
    Printf.sprintf
      {|
    %s;
    let input = JSON.parse(%s)
    if (input.custom) input.custom = JSON.parse(input.custom)
    else input.custom = {}
    let output = main(input)
    output.custom = JSON.stringify(output.custom)
    JSON.stringify(output)
    |}
      (Js.to_string input##.p1_code)
      input_var
  in
  let run robot_input =
    Lwt.wrap (fun () ->
        Js.Unsafe.set input##.realm##.global input_var robot_input;
        input##.realm##evaluate code |> Js.to_string )
  in
  Logic.start run >|= Js.string >|= callback

let _ = Js.export "main" main
