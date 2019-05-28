port module Page.Robot exposing (Model, Msg, init, update, view, subscriptions)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Json.Decode as Decode

import Data
import Component.Game as Game


-- MODEL

type alias Model =
    { name : String
    , code : String
    , gameState : GameState
    , totalTurns : Int
    }

type GameState
    = Loading Int
    | Game Game.Model | Error Data.Error | NoGame | InternalError


init : String -> String -> Int -> ( Model, Cmd Msg )
init user robot totalTurns =
    ( Model "" "" NoGame totalTurns, Cmd.none )


-- UPDATE

port getProgress : (Int -> msg) -> Sub msg
port getOutput : (Decode.Value -> msg) -> Sub msg
port getError : (() -> msg) -> Sub msg

subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.batch [ getOutput GotOutput,
        getProgress GotProgress,
        getError (always GotError)
    ]

port startEval : String -> Cmd msg
port reportDecodeError : String -> Cmd msg

type Msg
    = Run
    | CodeChanged String

    | GotOutput Decode.Value
    | GotProgress Int
    | GotError

    | GameMsg Game.Msg


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        Run ->
            ( { model | gameState = Loading 0 }, startEval model.code )

        CodeChanged code ->
            ( { model | code = code }, Cmd.none )


        GotOutput output ->
          case Data.decodeOutput output of
            Ok data ->
              ( { model | gameState =
              case data of
                Ok outcome ->
                  Game { data = outcome, turn = 0 }
                Err error ->
                  Error error
              }, Cmd.none )

            Err error ->
              ( { model | gameState = InternalError }, reportDecodeError <| Decode.errorToString error )

        GotProgress turn ->
            ( { model | gameState = Loading turn }, Cmd.none)

        GotError ->
            ( { model | gameState = InternalError }, Cmd.none )


        GameMsg gameMsg ->
            case model.gameState of
                Game gameModel -> ( { model | gameState =
                        Game <| Game.update gameMsg gameModel
                    }, Cmd.none )
                _ -> ( model, Cmd.none )


-- VIEW

to_perc : Float -> String
to_perc float =
    String.fromFloat float ++ "%"

view : Model -> ( String, Html Msg )
view model =
    ( "Robot Rumble", viewUI model )

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
        ] ++ case model.gameState of
            Error error ->
                case error.errorLoc of
                Just errorLoc ->
                    [property "errorLoc" <|
                        Data.errorLocEncoder errorLoc]
                Nothing -> []
            _ -> []
        )
        []

viewGame : Model -> Html Msg
viewGame model =
    div [ style "width" "40%"
        , style "max-width" "500px"
        ]
        [ viewBar model
        , viewViewer model
        ]

viewBar : Model -> Html Msg
viewBar model =
    div [ class "progress-holder" ]
        [ case model.gameState of
            Loading turn ->
                let progress_perc = (toFloat turn) / (toFloat model.totalTurns) * 100 in
                div [class "progress", class "mb-3", style "width" <| to_perc progress_perc] []
            _ -> div [] []
           ,  button [onClick Run, class "button", class "mb-3"
                 , style "visibility" <|
                     case model.gameState of
                        Loading turn -> "hidden"
                        _ -> "visible"
                 ] [text "run"]

        ]

viewViewer : Model -> Html Msg
viewViewer model =
    case model.gameState of
        Game gameModel ->
            Game.view gameModel |> toRootMsg

        Error error ->
            div []
                [ Game.viewEmpty |> toRootMsg
                , p [class "error", class "mt-3"] [text error.message]
                ]

        InternalError ->
            div []
                [ Game.viewEmpty |> toRootMsg
                , p [class "internal-error", class "mt-3"] [text "Internal Error! Please try again later."]
                ]

        _ ->
            Game.viewEmpty |> toRootMsg

toRootMsg : Html Game.Msg -> Html Msg
toRootMsg = Html.map GameMsg
