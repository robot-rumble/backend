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
    , opponent : Opponent
    , publishStatus : PublishStatus
    , inputUser : String
    , inputRobot : String
    , error : Maybe String
    }

type PublishStatus
    = None
    | Publishing
    | Published
    | NotAllowed

type Opponent
    = Yourself
    | Robot Api.Robot

type GameState
    = Loading Int
    | ChoosingOpponent
    | Game Game.Model
    | Error Data.Error
    | NoGame
    | InternalError


init : Auth.Auth -> Maybe Api.Robot -> Int -> ( Model, Cmd Msg )
init auth maybeRobot totalTurns =
    let code = case maybeRobot of
            Just robot -> robot.code
            Nothing -> ""
    in
    ( Model auth code NoGame totalTurns maybeRobot Yourself None "" "" Nothing, Cmd.none )


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

port startEval : (String, String, Int) -> Cmd msg
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

    | StartChoosingOpponent
    | ChooseOpponent Opponent
    | SearchOpponent
    | GotOpponent (Result Api.Error Api.Robot)

    | GotInput Input

    | ChangeTotalTurns String

type Input
    = UserInput String
    | RobotInput String

update : Msg -> Model -> Api.Key -> ( Model, Cmd Msg )
update msg model apiKey =
    case msg of
        Run ->
            ( { model | gameState = Loading 0 }, startEval (model.code, case model.opponent of
                Yourself -> model.code
                Robot robot -> robot.code
            , model.totalTurns))

        Save ->
            case (model.robot, model.auth) of
                (Just robot, Auth.LoggedIn auth) -> (
                        { model | publishStatus = Publishing },
                        Api.updateRobot robot.id { jwt = auth.jwt, code = model.code } SaveDone apiKey
                    )
                (_, _) -> (model, Cmd.none)

        SaveDone result -> case result of
            Ok _ -> ({ model | publishStatus = Published }, Cmd.none)
            Err _ -> ({ model | publishStatus = NotAllowed }, Cmd.none)

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

        StartChoosingOpponent ->
            ( { model | gameState = case model.gameState of
                ChoosingOpponent -> NoGame
                _ -> ChoosingOpponent
            }, Cmd.none )

        ChooseOpponent opponent ->
            ( { model | opponent = opponent, gameState = NoGame }, Cmd.none )

        SearchOpponent ->
            ( model, Api.getRobot model.inputUser model.inputRobot GotOpponent apiKey)

        GotOpponent result -> case result of
            Ok robot -> ( { model | opponent = Robot robot, gameState = NoGame, inputUser = "", inputRobot = "" }, Cmd.none)
            Err _ -> ( { model | error = Just "Robot does not exist." }, Cmd.none)

        GotInput input -> (
            case input of
                UserInput user ->
                    { model | inputUser = user }

                RobotInput robot ->
                    { model | inputRobot = robot }
            , Cmd.none)

        ChangeTotalTurns turns ->
            case String.toInt turns of
                Just int -> ( { model | totalTurns = int }, Cmd.none )
                Nothing -> ( model, Cmd.none )


-- VIEW

to_perc : Float -> String
to_perc float =
    String.fromFloat float ++ "%"

view : Model -> ( String, Html Msg, Html Msg )
view model =
    ( "Robot Rumble", viewHeader model, viewUI model )

viewHeader : Model -> Html Msg
viewHeader model =
    case model.robot of
        Nothing -> div [] [ p [] [text "Robot Rumble Demo"] ]
        Just robot -> div [ class "d-flex align-items-center" ] (
            [ p [class "m-0 mr-3"] [text robot.name] ]
            ++ case (model.auth) of
                (Auth.LoggedIn _) -> [
                        button [onClick Save, class "button", class "mr-3"] [text "publish"],
                        p [class "m-0 text-muted"] [text <| case model.publishStatus of
                                Publishing -> "loading..."
                                Published -> "published"
                                NotAllowed -> "not allowed!"
                                None -> ""
                        ]
                    ]
                _ -> []
            )

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
          , div [ class "d-flex"
                , class "justify-content-between"
                , style "visibility" <|
                         case model.gameState of
                            Loading turn -> "hidden"
                            _ -> "visible"
                , class "mb-3"
                ]
                [ div [
                     class "d-flex"
                    , class "align-items-center"
                    ]
                    [ button [onClick Run, class "button"] [text "run"]
                    , p [class "mx-3 my-0"] [text "vs"]
                    , button [ onClick StartChoosingOpponent
                             , class "a"
                             , class "text-red"
                             , disabled <| case model.robot of
                                Just _ -> False
                                Nothing -> True
                             ]
                             [ text <| case model.opponent of
                                Yourself -> "yourself"
                                Robot robot -> robot.name
                            ]
                    ]
                ,   div [] [
                        input [style "max-width" "4rem", class "py-0", value <| String.fromInt model.totalTurns, type_ "number", onInput ChangeTotalTurns] []
                    ]
                ]

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

        ChoosingOpponent ->
            div [ class "mt-4" ]
                [ button [class "a", class "mb-3", onClick <| ChooseOpponent Yourself] [text "yourself"]
                , div [class "d-flex", class "list"]
                    [ input [class "input", style "max-width" "9rem", value model.inputUser, onInput (UserInput >> GotInput), placeholder "user" ] []
                    , input [class "input", style "max-width" "9rem", value model.inputRobot, onInput (RobotInput >> GotInput), placeholder "robot" ] []
                    , button [class "button", onClick SearchOpponent] [text "select"]
                    ]
                , case model.error of
                    Just error -> p [class "error"] [text error]
                    Nothing -> div [] []
                ]

        _ ->
            Game.viewEmpty |> toRootMsg

toRootMsg : Html Game.Msg -> Html Msg
toRootMsg = Html.map GameMsg
