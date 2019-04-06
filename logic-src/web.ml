open Js_of_ocaml

class type realm =
  object
    method global : Js.Unsafe.any Js.t Js.prop

    method evaluate : string -> string Js.meth
  end

class type input =
  object
    method p1_code : Js.js_string Js.t Js.readonly_prop

    method realm : realm Js.t Js.readonly_prop
  end

let main (input : input Js.t) =
  let run code state =
    Js.Unsafe.set input##.realm##.global "state" state;
    input##.realm##evaluate code
  and code =
    Printf.sprintf "%s;JSON.stringify(main(JSON.parse(state)))"
    @@ Js.to_string input##.p1_code
  in
  Logic.start run {p1_code= code}

let _ = Js.export "main" main
