port module Ports exposing (..)

import Json.Decode as Decode exposing (Decoder, int, string, float)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)


type alias Unit =
  { class : String
  , x : Int
  , y : Int
  , health : Int
  }

port sendInput : Decoder Unit -> Cmd msg

type Ms
  = Searched String
  | Changed E.Value

port activeUsers : (E.Value -> msg) -> Sub msg

