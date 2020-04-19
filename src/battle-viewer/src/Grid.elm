module Grid exposing (Msg(..), view)

import Data
import Dict
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)


to_perc : Float -> String
to_perc float =
    String.fromFloat float ++ "%"


map_size =
    19


max_health =
    5



-- UPDATE


type Msg
    = UnitSelected Data.Id



-- VIEW


view : Maybe ( Data.TurnState, Maybe Data.Id ) -> Html Msg
view maybeState =
    let
        gridTemplateRows =
            "repeat(" ++ String.fromInt map_size ++ ", 1fr)"

        gridTemplateColumns =
            "repeat(" ++ String.fromInt map_size ++ ", 1fr)"
    in
    div [ class "_renderer-wrapper" ]
        [ div
            [ class "_renderer"
            , style "grid-template-rows" gridTemplateRows
            , style "grid-template-columns" gridTemplateColumns
            ]
          <|
            List.append gameGrid
                (case maybeState of
                    Just ( state, selectedId ) ->
                        gameObjs state selectedId

                    Nothing ->
                        []
                )
        ]


gameGrid : List (Html Msg)
gameGrid =
    List.append
        (List.range 1 map_size
            |> List.map
                (\y ->
                    div [ class "grid-row", style "grid-area" <| "1 / " ++ String.fromInt y ++ "/ end / auto" ] []
                )
        )
        (List.range 1 map_size
            |> List.map
                (\x ->
                    div [ class "grid-col", style "grid-area" <| String.fromInt x ++ "/ 1 / auto / end" ] []
                )
        )


gameObjs : Data.TurnState -> Maybe Data.Id -> List (Html Msg)
gameObjs state selectedUnit =
    Dict.values state.objs
        |> List.map
            (\( basic, details ) ->
                let
                    ( x, y ) =
                        basic.coords
                in
                div
                    ([ class "obj"
                     , class basic.id
                     , style "grid-column" <| String.fromInt (x + 1)
                     , style "grid-row" <| String.fromInt (y + 1)
                     ]
                        ++ (case details of
                                Data.UnitDetails unit ->
                                    [ class "unit"
                                    , class <| "team-" ++ unit.team
                                    , onClick (UnitSelected basic.id)
                                    , class <|
                                        case selectedUnit of
                                            Just id ->
                                                if id == basic.id then
                                                    "selected"

                                                else
                                                    ""

                                            Nothing ->
                                                ""
                                    ]

                                Data.TerrainDetails terrain ->
                                    [ class "terrain"
                                    , class <|
                                        "type-"
                                            ++ (case terrain.type_ of
                                                    Data.Wall ->
                                                        "wall"
                                               )
                                    ]
                           )
                    )
                    [ case details of
                        Data.UnitDetails unit ->
                            let
                                health_perc =
                                    toFloat unit.health / toFloat max_health * 100
                            in
                            div
                                [ class "health-bar"
                                , style "width" <| to_perc health_perc
                                , style "height" <| to_perc health_perc
                                ]
                                []

                        _ ->
                            div [] []
                    ]
            )
