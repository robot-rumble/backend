module Decode exposing (..)

import Basics
import Dict exposing (Dict)
import Array exposing (Array)
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (required, custom)
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


type alias Output = Result CompileError Outcome

type alias Outcome =
    { winner : String
    , turns : Array State
    }

type alias CompileError =
    { message : String
    , col : Maybe Int
    , row : Maybe Int
    }


outputDecoder : Decoder Output
outputDecoder =
    oneOf
        [ succeed Outcome
            |> required "winner" string
            |> required "turns" (array stateDecoder)
            |> map Ok
        , succeed CompileError
            |> required "message" string
            |> custom (field "col" (nullable int))
            |> custom (field "row" (nullable int))
            |> Debug.log "lo"
            |> map Err
        ]


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


type ObjDetails = UnitDetails Unit | TerrainDetails Terrain


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
        |> map UnitDetails


type alias Terrain =
    { type_ : TerrainType
    }

type TerrainType
    = Wall

terrainDecoder : Decoder ObjDetails
terrainDecoder =
    succeed Terrain
        |> required "type_" (string |> stringAsUnion [ ( "Wall", Wall ) ])
        |> map TerrainDetails

