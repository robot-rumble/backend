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
import Json.Encode


-- MAIN


main : Program Flags Model Msg
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
    , code : String
    , renderState : RenderState
    , totalTurns : Int
    }

type RenderState = Loading Int | Render RenderStateVal | Error RR.Error | NoRender | InternalError

type alias RenderStateVal =
   { data : RR.Outcome
   , turn : Int
   }



init : Flags -> Url.Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url key =
    (Model key url "" NoRender flags.totalTurns, Cmd.none )


type alias Flags =
    { totalTurns: Int
    }


-- UPDATE


port startEval : String -> Cmd msg
port reportDecodeError : String -> Cmd msg

type Msg
    = LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | GotOutput Decode.Value
    | GotProgress Int
    | Run
    | GotRenderMsg RenderMsg
    | CodeChanged String
    | GotError

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
              ( { model | renderState =
              case data of
                Ok outcome ->
                  Render { data = outcome, turn = 0 }
                Err error ->
                  Error error
              }, Cmd.none )

            Err error ->
              ( model, reportDecodeError <| Decode.errorToString error )

        GotProgress turn ->
            ( { model | renderState = Loading turn }, Cmd.none)

        Run ->
            ( { model | renderState = Loading 0 }, startEval model.code )

        GotRenderMsg renderMsg ->
            case model.renderState of
                Render state -> ( { model | renderState = Render <| updateRender renderMsg state }, Cmd.none )
                _ -> ( model, Cmd.none )

        CodeChanged code ->
            ( { model | code = code }, Cmd.none )

        GotError ->
            ( { model | renderState = InternalError }, Cmd.none )

updateRender : RenderMsg -> RenderStateVal -> RenderStateVal
updateRender msg model =
    case msg of
        ChangeTurn dir -> ( { model | turn = model.turn +
            case dir of
                Next -> 1
                Previous -> -1
            } )


-- SUBSCRIPTIONS

port getProgress : (Int -> msg) -> Sub msg
port getOutput : (Decode.Value -> msg) -> Sub msg
port getError : (() -> msg) -> Sub msg

subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.batch [
        getOutput GotOutput,
        getProgress GotProgress,
        getError (always GotError)
    ]

-- VIEW


to_perc : Float -> String
to_perc float =
    String.fromFloat float ++ "%"


view : Model -> Browser.Document Msg
view model =
    { title = "Robot Rumble"
    , body = [viewUI model]
    }

viewUI : Model -> Html Msg
viewUI model =
    div []
        [ p [ class "mt-5"
            , class "w-75"
            , class "mx-auto"
            ] [text "Welcome to Robot Rumble! This demo allows you to code a robot and run it against itself. The robot's code is a function that returns the type and direction of an action. The arena on the right is a way to battle the robot against itself. The code is open source at https://github.com/chicode/robot-rumble."]
        , div
          [ class "d-flex"
          , class "justify-content-around"
          , class "mt-6"
          , class "mx-6"
          ] [ viewEditor model
            , viewGame model
            ]
        ]

viewEditor : Model -> Html Msg
viewEditor model =
    Html.node "code-editor"
        ([ Html.Events.on "editorChanged" <|
            Decode.map CodeChanged <|
                Decode.at [ "target", "value" ] <|
                    Decode.string
        , style "width" "60%"
        , class "pr-6"
        ] ++ case model.renderState of
            Error error ->
                case error.errorLoc of
                Just errorLoc ->
                    [property "errorLoc" <|
                        RR.errorLocEncoder errorLoc]
                Nothing -> []
            _ -> []
        )
        []

viewGame : Model -> Html Msg
viewGame model =
    div [ style "width" "40%"
        , style "max-width" "500px"
        ] (case model.renderState of
            InternalError ->
                [p [class "internal-error"] [text "Internal Error! Please check back soon."]]
            _ ->
                [ viewGameBar model
                , viewGameViewer model
                ]
        )


viewGameBar : Model -> Html Msg
viewGameBar model =
    div [ class "progress-holder" ]
        [ case model.renderState of
            Loading turn ->
                let progress_perc = (toFloat turn) / (toFloat model.totalTurns) * 100 in
                div [class "progress", class "mb-3", style "width" <| to_perc progress_perc] []
            _ -> div [] []
           ,  button [onClick Run, class "button", class "mb-3"
                 , style "visibility" <|
                     case model.renderState of
                        Loading turn -> "hidden"
                        _ -> "visible"
                 ] [text "run"]

        ]

viewGameViewer : Model -> Html Msg
viewGameViewer model =
    case model.renderState of
        Render state ->
            let game =
                    case Array.get state.turn state.data.turns of
                       Just turn -> gameRenderer (gameObjs turn)
                       Nothing -> div [] [text "Invalid turn."]
            in
            div []
                [ game
                , div [class "d-flex", class "justify-content-center", class "mt-3"]
                  [ button
                        [onClick <| GotRenderMsg (ChangeTurn Previous)
                        , disabled (state.turn == 0)
                        , class "arrow-button"
                        ] [text "\u{2190}"]
                  , div [style "width" "6rem", class "text-center"] [text <| "turn " ++ String.fromInt (state.turn + 1)]
                  , button
                        [onClick <| GotRenderMsg (ChangeTurn Next)
                        , disabled (state.turn == Array.length state.data.turns - 1)
                        , class "arrow-button"
                        ] [text "\u{2192}"]
                  ]
            ]

        Error error ->
            div []
                [ gameRenderer []
                , p [class "error", class "mt-3"] [text error.message]
                ]

        _ ->
            gameRenderer []


map_size = 19
max_health = 5

gameObjs : RR.State -> List (Html Msg)
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
                RR.UnitDetails unit ->
                   [ class "unit"
                   , class <| "team-" ++ unit.team
                   ]
                RR.TerrainDetails terrain ->
                   [ class "terrain"
                   , class <| "type-" ++ (
                      case terrain.type_ of
                         RR.Wall -> "wall"
                      )
                   ]
             ))
            [
             case details of
                RR.UnitDetails unit ->
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
gameGrid =
    List.append
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
