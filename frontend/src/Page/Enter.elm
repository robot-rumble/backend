module Page.Enter exposing (Model, Msg, init, subscriptions, update, view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Json.Decode as Decode

import Route

import Auth
import Api


-- MODEL

type alias Model =
    { auth : Auth.Auth
    , key : Route.Key
    , username : Maybe String
    , password : Maybe String
    , email : Maybe String
    , error : Maybe String
    }


init : Auth.Auth -> Route.Key -> ( Model, Cmd Msg )
init auth key =
    ( Model auth key Nothing Nothing Nothing Nothing, Cmd.none )



-- UPDATE


subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.batch []


type Msg
    = GotInput Input
    | SignUp
    | LogIn
    | GotSignup (Result Api.Error Api.User)
    | GotLogin (Result Api.Error Api.AuthUser)

type Input
    = Password String
    | Username String
    | Email String


update : Msg -> Model -> ( Model, Cmd Msg, Auth.AuthCmd )
update msg model =
    case msg of
        GotInput input -> (
            case input of
                Username username ->
                    { model | username = Just username }

                Password password ->
                    { model | password = Just password }

                Email email ->
                    { model | email = Just email }
            , Cmd.none, Auth.None)

        SignUp ->
            case (model.username, model.password, model.email) of
                (Just username, Just password, Just email) ->
                    (model, Api.signUp (Api.SignUpBody username password email) GotSignup, Auth.None)
                _ ->
                    ({ model | error = Just "missing fields" }, Cmd.none, Auth.None)

        LogIn ->
            case (model.username, model.password) of
                (Just username, Just password) ->
                    (model, Api.logIn (Api.LogInBody username password) GotLogin, Auth.None)
                _ ->
                    ({ model | error = Just "missing fields" }, Cmd.none, Auth.None)

        GotSignup result ->
            case result of
                Ok _ -> (model, Route.push model.key Route.Home, Auth.None)
                Err err ->
                    let _ = Debug.log "err" err in
                    ({ model | error = Just "one of your inputs is bad" }, Cmd.none, Auth.None)

        GotLogin result ->
            case result of
                Ok authUser -> (model, Route.push model.key <| Route.User authUser.user.username, Auth.LogIn authUser)
                Err _ -> ({ model | error = Just "invalid username or password" }, Cmd.none, Auth.None)

-- VIEW


view : Model -> ( String, Html Msg, Html Msg )
view model =
    ( "Robot Rumble", div [] [], viewBody model )


viewBody : Model -> Html Msg
viewBody model =
    div [class "form"]
        [ input [ placeholder "username", value <| Maybe.withDefault "" model.username, onInput (Username >> GotInput) ] []
        , input [ placeholder "password",  value <| Maybe.withDefault "" model.password, onInput (Password >> GotInput) ] []
        , button [ onClick LogIn, class "button", class "mb-4" ] [ text "login" ]
        , input [ placeholder "email", value <| Maybe.withDefault "" model.email, onInput (Email >> GotInput) ] []
        , button [ onClick SignUp, class "button" ] [ text "signup" ]
        , case model.error of
            Just error -> p [ class "error" ] [text error]
            Nothing -> div [] []
        ]
