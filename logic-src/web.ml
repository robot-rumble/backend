open Js_of_ocaml

class type realm = object
  method global : Js.Unsafe.any Js.t Js.prop
  method evaluate : string -> Js.Unsafe.any Js.meth
end

class type input = object
  method p1 : string Js.readonly_prop
  method realm : realm Js.t Js.readonly_prop
end

let main (input : input Js.t) =
  let run code state =
    Js.Unsafe.set input##.realm##.global "state" state;
    let result = input##.realm##evaluate code in
    result
  in
  Main.start run {p1_code = input##.p1}

let _ =
  Js.export "main" main
