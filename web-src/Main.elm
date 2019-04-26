port module Main exposing (..)

import Browser
import Browser.Dom
import Browser.Events
import Browser.Navigation as Nav
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Task
import Url
import Array

import Dict
import Tuple exposing (first, second)

import Decode as RR
import Json.Decode as Decode

-- MAIN


main : Program () Model Msg
main =
    Browser.application
        { init = init
        , view = view
        , update = update
        , subscriptions = subscriptions
        , onUrlChange = UrlChanged
        , onUrlRequest = LinkClicked
        }



-- MODEL


type alias Model =
    { key : Nav.Key
    , url : Url.Url
    , renderState : Maybe RenderState
    }

type alias RenderState =
   { data : RR.Output
   , turn : Int
   }


init : () -> Url.Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url key =
    (Model key url Nothing, Cmd.none )



-- UPDATE

port startEval : String -> Cmd msg

type Msg
    = LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | GotOutput Decode.Value
    | Run
    | GotRenderMsg RenderMsg

type RenderMsg = ChangeTurn Direction
type Direction = Next | Previous

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        LinkClicked urlRequest ->
            case urlRequest of
                Browser.Internal url ->
                    ( model, Nav.pushUrl model.key (Url.toString url) )

                Browser.External href ->
                    ( model, Nav.load href )

        UrlChanged url ->
            ( { model | url = url }, Cmd.none )

        GotOutput output ->
          case RR.decodeOutput output of
            Ok data ->
              ( { model | renderState = Just { data = data, turn = 0 } }, Cmd.none )

            Err error ->
              let _ = Debug.log "Elm Error" <| Debug.toString error in
              ( model, Cmd.none )


        Run ->
            ( model, startEval """
   const displayMap = (objs, map) =>
     map
       .map((col) =>
         col
           .map((id) => {
             if (id) {
               if (objs[id].type_ === 'Wall') return 'wall '
               else return id
             } else {
               return '     '
             }
           })
           .join(' '),
       )

       .join(`\n`)

	function main (input) {
        let actions = {}
        console.log(input)
        console.log(displayMap(input.state.objs, input.state.map))

        for (let id of input.state.teams[input.team]) {
            actions[id] = { type_: "Move", direction: input.team == "red" ? "Right" : "Down" }
        }

		return { actions }
	}
            """ )

        GotRenderMsg renderMsg ->
            case model.renderState of
                Just state -> ( { model | renderState = Just <| updateRender renderMsg state }, Cmd.none )
                Nothing -> ( model, Cmd.none )

updateRender : RenderMsg -> RenderState -> RenderState
updateRender msg model =
    case msg of
        ChangeTurn dir -> ( { model | turn = model.turn +
            case dir of
                Next -> 1
                Previous -> -1
            } )


-- SUBSCRIPTIONS

port getOutput : (Decode.Value -> msg) -> Sub msg

subscriptions : Model -> Sub Msg
subscriptions _ =
   getOutput GotOutput

-- VIEW


view : Model -> Browser.Document Msg
view model =
    { title = "Copala"
    , body =
        [ button [onClick Run, class "mx-auto", class "d-block", class "mt-5"] [text "run"]
        , case model.renderState of
            Just output -> viewUI output
            _ -> div [] []
        ]
    }

viewUI : RenderState -> Html Msg
viewUI state =
    let game =
            case Array.get state.turn state.data.turns of
               Just turn -> viewGame turn
               Nothing -> div [] [text "Invalid turn."]
    in
    div
      [ class "d-flex"
      , class "mt-6"
      ] [
       div [ class "mx-auto"]
           [ game
           , div [] [text <| "current turn: " ++ String.fromInt (state.turn + 1)]
           , button
            [onClick <| GotRenderMsg (ChangeTurn Next)
            , disabled (state.turn == Array.length state.data.turns - 1)
            ] [text "next turn"]
           , button
            [onClick <| GotRenderMsg (ChangeTurn Previous)
            , disabled (state.turn == 0)] [text "previous turn"]
       ]
    ]


map_width = 10
map_height = 10

image : String -> Html.Attribute Msg
image name =
   style "background-image" <| "url(\"" ++ name ++ ".png\")"

viewGame : RR.State -> Html Msg
viewGame state =
    let obj_divs =
            Dict.values state.objs
            |> List.map (\(basic, details) ->
                let (x, y) = basic.coords in
                div ([ class "obj"
                     , class basic.id
                     , style "grid-column" <| String.fromInt (x + 1)
                     , style "grid-row" <| String.fromInt (y + 1)
                    ] ++ (
                     case details of
                        RR.UnitObj unit ->
                           [ class "unit"
                           , class <| "team-" ++ unit.team
                           , image <| "soldier-" ++ unit.team
                           ]
                        RR.TerrainObj terrain ->
                           [ class "terrain"
                           , class <| "type-" ++ (
                              case terrain.type_ of
                                 RR.Wall -> "wall"
                              )
                           , image "wall"
                           ]
                     ))
                    []
            )
        gridTemplateRows = "repeat(" ++ String.fromInt map_width ++ ", 1fr)"
        gridTemplateColumns = "repeat(" ++ String.fromInt map_height ++ ", 1fr)"
    in
    div [class "renderer"
        , style "grid-template-rows" gridTemplateRows
        , style "grid-template-columns" gridTemplateColumns
        ] obj_divs
