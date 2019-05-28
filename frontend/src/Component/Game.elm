module Component.Game exposing (Model, Msg, update, view, viewEmpty)

import Array
import Dict

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)

import Data


-- MODEL

type alias Model =
    { data : Data.Outcome
    , turn : Int
    }

type Msg = ChangeTurn Direction
type Direction = Next | Previous


-- UPDATE

update : Msg -> Model -> Model
update msg model =
    case msg of
        ChangeTurn dir -> ( { model | turn = model.turn +
            case dir of
                Next -> 1
                Previous -> -1
            } )


-- VIEW

map_size = 19
max_health = 5

to_perc : Float -> String
to_perc float =
    String.fromFloat float ++ "%"

view : Model -> Html Msg
view model =
    let game = case Array.get model.turn model.data.turns of
           Just turn -> gameRenderer (gameObjs turn)
           Nothing -> div [] []
    in
    div []
        [ game
        , div [class "d-flex", class "justify-content-center", class "mt-3"]
          [ button
                [onClick <| ChangeTurn Previous
                , disabled (model.turn == 0)
                , class "arrow-button"
                ] [text "\u{2190}"]
          , div [style "width" "6rem", class "text-center"] [text <| "turn " ++ String.fromInt (model.turn + 1)]
          , button
                [onClick <| ChangeTurn Next
                , disabled (model.turn == Array.length model.data.turns - 1)
                , class "arrow-button"
                ] [text "\u{2192}"]
          ]
    ]


viewEmpty : Html Msg
viewEmpty = gameRenderer []


gameObjs : Data.State -> List (Html Msg)
gameObjs state =
    Dict.values state.objs
    |> List.map (\(basic, details) ->
        let (x, y) = basic.coords in
        div ([ class "obj"
             , class basic.id
             , style "grid-column" <| String.fromInt (x + 1)
             , style "grid-row" <| String.fromInt (y + 1)
            ] ++ (
             case details of
                Data.UnitDetails unit ->
                   [ class "unit"
                   , class <| "team-" ++ unit.team
                   ]
                Data.TerrainDetails terrain ->
                   [ class "terrain"
                   , class <| "type-" ++ (
                      case terrain.type_ of
                         Data.Wall -> "wall"
                      )
                   ]
             ))
            [
             case details of
                Data.UnitDetails unit ->
                   let health_perc = (toFloat unit.health) / (toFloat max_health) * 100
                   in
                   div
                      [ class "health-bar"
                      , style "width" <| to_perc health_perc
                      , style "height" <| to_perc health_perc
                      ] []
                _ -> div [] []
            ]

    )

gameGrid : List (Html Msg)
gameGrid = List.append
        (List.range 1 map_size |> List.map (\y ->
            div [class "grid-row", style "grid-area" <| "1 / " ++ (String.fromInt y) ++ "/ end / auto"] []
        ))
        (List.range 1 map_size |> List.map (\x ->
            div [class "grid-col", style "grid-area" <| (String.fromInt x) ++ "/ 1 / auto / end"] []
        ))

-- accepts divs to display in the renderer
gameRenderer : List (Html Msg) -> Html Msg
gameRenderer divs =
    let gridTemplateRows = "repeat(" ++ String.fromInt map_size ++ ", 1fr)"
        gridTemplateColumns = "repeat(" ++ String.fromInt map_size ++ ", 1fr)"
    in
    div [class "renderer-wrapper"] [
        div [class "renderer"
            , style "grid-template-rows" gridTemplateRows
            , style "grid-template-columns" gridTemplateColumns
            ] <| List.append (gameGrid) divs
    ]
