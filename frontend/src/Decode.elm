module Decode exposing (Coords, Id, Output, State, Team, Unit, UnitType, Terrain, TerrainType(..), ObjDetails(..), decodeOutput)

import Basics
import Dict exposing (Dict)
import Array exposing (Array)
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (required)
import Json.Encode as Encode

arrayAsTuple2 : Decoder a -> Decoder b -> Decoder ( a, b )
arrayAsTuple2 a b =
    index 0 a
        |> andThen
            (\aVal ->
                index 1 b
                    |> andThen (\bVal -> Json.Decode.succeed ( aVal, bVal ))
            )


stringAsUnion : List ( String, a ) -> Decoder String -> Decoder a
stringAsUnion mapping =
    let
        dict =
            Dict.fromList mapping
    in
    andThen
        (\str ->
            case Dict.get str dict of
                Just a ->
                    succeed a

                Nothing ->
                    fail ("Invalid type: " ++ str)
        )


unionDecoder : List ( String, a ) -> List ( String, String -> a ) -> Decoder a
unionDecoder plainMappings valueMappings =
    let
        decodePlain =
            string |> stringAsUnion plainMappings

        decodeWithValue =
            index 0 string |> stringAsUnion valueMappings |> andThen (\unionType -> map unionType <| index 1 string)
    in
    oneOf [ decodePlain, decodeWithValue ]


type alias Id =
    String


type alias Coords =
    ( Int, Int )


type alias Team =
    String


decodeOutput : Value -> Result Error Output
decodeOutput =
    decodeValue outputDecoder


type alias Output =
    { winner : String
    , turns : Array State
    }


outputDecoder : Decoder Output
outputDecoder =
    succeed Output
        |> required "winner" string
        |> required "turns" (array stateDecoder)


type alias State =
    { turn : Int
    , objs : Dict String Obj
    }


stateDecoder : Decoder State
stateDecoder =
    succeed State
        |> required "turn" int
        |> required "objs" (dict objDecoder)

type alias Obj = ( BasicObj, ObjDetails )

objDecoder : Decoder Obj
objDecoder =
    field "type_" string
    |> andThen (\type_ ->
        case type_ of
            "Soldier" -> unitDecoder
            "Wall" -> terrainDecoder
            _ -> fail ("Invalid type: " ++ type_)
        )
    |> andThen (\details ->
        basicObjDecoder |> andThen (\basic ->
            succeed (basic, details)
        )
    )

type alias BasicObj =
    { coords : Coords
    , id : Id
    }

basicObjDecoder : Decoder BasicObj
basicObjDecoder =
    succeed BasicObj
        |> required "coords" (arrayAsTuple2 int int)
        |> required "id" string


type ObjDetails = UnitObj Unit | TerrainObj Terrain


type alias Unit =
    { type_ : UnitType
    , health : Int
    , team : Team
    }

type UnitType
    = Soldier

unitDecoder : Decoder ObjDetails
unitDecoder =
    succeed Unit
        |> required "type_" (string |> stringAsUnion [ ( "Soldier", Soldier ) ])
        |> required "health" int
        |> required "team" string
        |> map UnitObj


type alias Terrain =
    { type_ : TerrainType
    }

type TerrainType
    = Wall

terrainDecoder : Decoder ObjDetails
terrainDecoder =
    succeed Terrain
        |> required "type_" (string |> stringAsUnion [ ( "Wall", Wall ) ])
        |> map TerrainObj

