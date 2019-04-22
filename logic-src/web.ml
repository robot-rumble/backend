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
	  (input) => {
		%s;
		return JSON.stringify(main(JSON.parse(input)))
	  }
    |}
      (Js.to_string input##.p1_code)
  in
  let func = input##.realm##evaluate code in
  let run (robot_input : string) =
    Lwt.wrap (fun () ->
        Js.Unsafe.fun_call func [|Js.Unsafe.inject robot_input|]
        |> Js.to_string )
  in
  Logic.start run >|= Js.string >|= callback

let _ = Js.export "main" main
