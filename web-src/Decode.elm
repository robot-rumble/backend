module Decode exposing (Output, decodeOutput)

import Json.Encode as Encode
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (required)
import Dict exposing (Dict)

import Basics

arrayAsTuple2 : Decoder a -> Decoder b -> Decoder (a, b)
arrayAsTuple2 a b =
    index 0 a
        |> andThen (\aVal -> index 1 b
        |> andThen (\bVal -> Json.Decode.succeed (aVal, bVal)))

stringAsUnion : List (String, a) -> Decoder String -> Decoder a
stringAsUnion mapping =
  let dict = Dict.fromList mapping in
  andThen (\str ->
    case Dict.get str dict of
      Just a -> succeed a
      Nothing -> fail ("Invalid type: " ++ str)
  )

unionDecoder : List (String, a) -> List (String, String -> a) -> Decoder a
unionDecoder plainMappings valueMappings =
  let
    decodePlain = string |> stringAsUnion plainMappings
    decodeWithValue = index 0 string |> stringAsUnion valueMappings |> andThen (\unionType -> map unionType <| index 1 string)
  in
  oneOf [decodePlain, decodeWithValue]

type alias Id = String
type alias Coords = (Int, Int)
type alias Team = String

decodeOutput : Value -> Result Error Output
decodeOutput = decodeValue outputDecoder

type alias Output =
  { winner : String
  }

outputDecoder : Decoder Output
outputDecoder =
    succeed Output
      |> required "winner" string

type alias State =
  { turn : Int
  , units : Dict String Unit
  , teams : Dict String (List Id)
  , map : Map
  }

stateDecoder : Decoder State
stateDecoder =
  succeed State
    |> required "turn" int
    |> required "units" (dict unitDecoder)
    |> required "teams" (dict (list string))
    |> required "map" mapDecoder


type UnitType = Soldier
type alias Unit =
  { type_ : UnitType
  , coords : Coords
  , health : Int
  , id : Id
  , team : Team
  }

unitDecoder : Decoder Unit
unitDecoder =
    succeed Unit
        |> required "type_" (string |> stringAsUnion [("Soldier", Soldier)])
        |> required "coords" (arrayAsTuple2 int int)
        |> required "health" int
        |> required "id" string
        |> required "team" string

type alias Map = List (List Tile)

mapDecoder : Decoder Map
mapDecoder =
  list (list tileDecoder)

type Tile = UnitTile Id | Wall | Empty

tileDecoder : Decoder Tile
tileDecoder =
  unionDecoder [("Empty", Empty), ("Wall", Wall)] [("Unit", UnitTile)]

