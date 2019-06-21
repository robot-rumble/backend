module Page.User exposing (Model, Msg, init, update, view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)

import Auth
import Api
import Route

import Browser.Navigation as Nav


-- MODEL

type alias Model =
    { auth : Auth.Auth
    , user : Api.User
    , key : Nav.Key
    , robotName : Maybe String
    , error : Maybe String
    }


init : Auth.Auth -> Api.User -> Nav.Key -> ( Model, Cmd Msg )
init auth user key =
    ( Model auth user key Nothing Nothing, Cmd.none )


-- UPDATE

type Msg
    = CreateRobot
    | GotRobot (Result Api.Error Api.Robot)
    | GotInput String


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        CreateRobot -> case model.robotName of
            Just name ->
                case model.auth of
                    Auth.LoggedIn auth ->
                        (model, Api.createRobot {
                            jwt = auth.jwt,
                            name = name
                        } GotRobot)
                    Auth.LoggedOut -> (model, Cmd.none)

            Nothing ->
                ({ model | error = Just "name can't be blank" }, Cmd.none)

        GotRobot result -> case result of
            Ok robot -> (model, Route.push model.key <| Route.Robot model.user.username robot.name)
            Err _ -> ({ model | error = Just "something went wrong" }, Cmd.none)

        GotInput robotName -> ({ model | robotName = Just robotName}, Cmd.none)

-- VIEW


view : Model -> ( String, Html Msg, Html Msg )
view model =
    ( model.user.username ++ "'s profile", div [] [ text model.user.username ], viewBody model )


viewBody : Model -> Html Msg
viewBody model =
    div []
        [ h1 [] [ text "Robots"]
        , div [] (
            List.map (\robot ->
                Route.a (Route.Robot model.user.username robot.name) [text robot.name]
              ) model.user.robots
          )
        , case model.auth of
            Auth.LoggedIn _ -> div []
                [ input [value <| Maybe.withDefault "" model.robotName, onInput GotInput] []
                , button [onClick CreateRobot] [text "create robot"]
                , case model.error of
                    Just error -> p [] [text error]
                    Nothing -> div [] []
                ]
            Auth.LoggedOut -> div [] []
        ]
