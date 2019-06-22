port module Page.Robot exposing (Model, Msg, init, update, view, subscriptions)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Json.Decode as Decode
import Json.Encode as Encode

import Data
import Component.Game as Game

import Auth
import Api


-- MODEL

type alias Model =
    { auth : Auth.Auth
    , code : String
    , gameState : GameState
    , totalTurns : Int
    , robot : Maybe Api.Robot
    , publishStatus : PublishStatus
    }

type PublishStatus
    = None
    | Publishing
    | Published

type GameState
    = Loading Int
    | Game Game.Model | Error Data.Error | NoGame | InternalError


init : Auth.Auth -> Maybe Api.Robot -> Int -> ( Model, Cmd Msg )
init auth maybeRobot totalTurns =
    let code = case maybeRobot of
            Just robot -> robot.code
            Nothing -> ""
    in
    ( Model auth code NoGame totalTurns maybeRobot None, Cmd.none )


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
    | Save
    | SaveDone (Result Api.Error Api.Robot)
    | CodeChanged String

    | GotOutput Decode.Value
    | GotProgress Int
    | GotError

    | GameMsg Game.Msg


update : Msg -> Model -> Api.Key -> ( Model, Cmd Msg )
update msg model apiKey =
    case msg of
        Run ->
            ( { model | gameState = Loading 0 }, startEval model.code )

        Save ->
            case (model.robot, model.auth) of
                (Just robot, Auth.LoggedIn auth) -> (
                        { model | publishStatus = Publishing },
                        Api.updateRobot robot.id { jwt = auth.jwt, code = model.code } SaveDone apiKey
                    )
                (_, _) -> (model, Cmd.none)

        SaveDone _ -> ({ model | publishStatus = Published }, Cmd.none)

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

view : Model -> ( String, Html Msg, Html Msg )
view model =
    ( "Robot Rumble", viewHeader model, viewUI model )

viewHeader : Model -> Html Msg
viewHeader model =
    div [] [text <| case model.robot of
        Just robot -> robot.name
        Nothing -> "Robot Rumble Demo"]

viewUI : Model -> Html Msg
viewUI model =
    div []
        [ case model.robot of
            Just robot -> div [] []
            Nothing ->
              p [ class "mb-5"
                , class "w-75"
                , class "mx-auto"
                ] [text "Welcome to Robot Rumble! This demo allows you to code a robot and run it against itself. The robot's code is a function that returns the type and direction of an action. The arena on the right is a way to battle the robot against itself."]
        , div
          [ class "d-flex"
          , class "justify-content-around"
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
        , property "name" <| Encode.string (
            case model.robot of
                Just robot -> robot.name
                Nothing -> "demo"
        )
        , property "lastEdit" <| Encode.int (
            case model.robot of
                Just robot -> robot.last_edit
                Nothing -> 0
        )
        , property "code" <| Encode.string model.code
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
          , div [style "visibility" <|
                     case model.gameState of
                        Loading turn -> "hidden"
                        _ -> "visible"
                , class "d-flex"
                , class "mb-3"
                , class "align-items-center"
                ] (
                [ button [onClick Run, class "button", class "mr-3"] [text "run"]]
                ++ case (model.robot, model.auth) of
                    (Just _, Auth.LoggedIn _) -> [
                            button [onClick Save, class "button", class "mr-3"] [text "publish"],
                            p [class "m-0"] [text <| case model.publishStatus of
                                    Publishing -> "loading..."
                                    Published -> "published"
                                    None -> ""
                            ]
                        ]
                    (_, _) -> []
                )

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
