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

import Data
import Route
import Json.Decode as Decode
import Json.Encode
import Http

import Page.Robot
import Page.Enter
import Page.User
import Page.Home

import Api
import Auth


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

type alias Model = ( BaseModel, PageModel )

type alias BaseModel =
    { routeKey : Nav.Key
    , apiKey : Api.Key
    , auth : Auth.Auth
    , totalTurns : Int
    }

type PageModel
    = RobotModel Page.Robot.Model
    | EnterModel Page.Enter.Model
    | UserModel Page.User.Model
    | HomeModel Page.Home.Model
    | NotFound
    | Error
    | Loading


-- INIT


type alias Flags =
    { totalTurns: Int
    , auth : Decode.Value
    , endpoint : String
    }

init : Flags -> Url.Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url routeKey =
    let auth = Auth.initAuth flags.auth in
    initPageModel url ( BaseModel routeKey flags.endpoint auth flags.totalTurns, Loading )


-- UPDATE

type Msg
    = LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | Page PageMsg
    | GotData DataRequest
    | Auth Auth.AuthCmd

type PageMsg
    = RobotMsg Page.Robot.Msg
    | EnterMsg Page.Enter.Msg
    | UserMsg Page.User.Msg
    | HomeMsg Page.Home.Msg

type DataRequest
    = User (Result Api.Error Api.User)
    | Robot (Result Api.Error Api.Robot)

update : Msg -> Model -> ( Model, Cmd Msg )
update rootMsg (( baseModel, pageModel ) as rootModel) =
    case rootMsg of
        LinkClicked urlRequest ->
            case urlRequest of
                Browser.Internal url ->
                    ( rootModel, Nav.pushUrl baseModel.routeKey (Url.toString url) )

                Browser.External href ->
                    ( rootModel, Nav.load href )

        UrlChanged url ->
            initPageModel url rootModel

        Page pageMsg ->
            updatePageModel pageMsg rootModel

        GotData request ->
            initDataPageModel request rootModel

        Auth authCmd ->
            let (auth, cmd) = Auth.processCmd authCmd baseModel.auth baseModel.routeKey in
            (({ baseModel | auth = auth }, pageModel), cmd)

initPageModel : Url.Url -> Model -> ( Model, Cmd Msg )
initPageModel url ( baseModel, pageModel ) =
    let toRoot newPageModel newPageMsg ( newModel, newCmd ) =
            ( newPageModel newModel, newCmd |> Cmd.map newPageMsg |> Cmd.map Page )
    in
    let ( newPageModel, newCmd ) = case Route.parse url of
            Nothing -> ( NotFound, Cmd.none )
            Just route -> case route of
                Route.Robot user robot -> (Loading, Api.getRobot user robot Robot baseModel.apiKey |> Cmd.map GotData)
                Route.User user -> (Loading, Api.getUser user User baseModel.apiKey |> Cmd.map GotData)
                Route.Demo -> Page.Robot.init baseModel.auth Nothing baseModel.totalTurns |> toRoot RobotModel RobotMsg
                Route.Enter -> Page.Enter.init baseModel.auth |> toRoot EnterModel EnterMsg
                Route.Home -> Page.Home.init |> toRoot HomeModel HomeMsg
                _ -> ( NotFound, Cmd.none )
    in
    ( (baseModel, newPageModel), newCmd )

initDataPageModel : DataRequest -> Model -> ( Model, Cmd Msg )
initDataPageModel request ( baseModel, pageModel ) =
    let toRoot newPageModel newPageMsg ( newModel, newCmd ) =
            ( newPageModel newModel, newCmd |> Cmd.map newPageMsg |> Cmd.map Page )
        handleError result f = case result of
            Ok data -> f data
            Err error -> (
                let _ = Debug.log "error" error in
                case error of
                    Http.BadStatus _ -> NotFound
                    _ -> Error
                , Cmd.none)
    in
    let ( newPageModel, newCmd ) = case request of
            User result -> handleError result (\user ->
                    Page.User.init baseModel.auth user baseModel.routeKey |> toRoot UserModel UserMsg
                )

            Robot result -> handleError result (\robot ->
                    Page.Robot.init baseModel.auth (Just robot) baseModel.totalTurns |> toRoot RobotModel RobotMsg
                )
    in
    ( (baseModel, newPageModel), newCmd )

updatePageModel : PageMsg -> Model -> ( Model, Cmd Msg )
updatePageModel pageMsg ( baseModel, pageModel ) =
    let toRoot newPageModel newPageMsg ( newModel, newCmd ) =
            ( baseModel, newPageModel newModel, newCmd |> Cmd.map newPageMsg |> Cmd.map Page )
        toAuth newPageModel newPageMsg ( newModel, newCmd, authCmd ) =
            let ( newAuth, newAuthCmd ) = Auth.processCmd authCmd baseModel.auth baseModel.routeKey
                newPageCmd = newCmd |> Cmd.map newPageMsg |> Cmd.map Page
            in
            ( { baseModel | auth = newAuth }, newPageModel newModel, Cmd.batch [newPageCmd, newAuthCmd] )
    in
    let ( newBaseModel, newPageModel, newCmd ) = case ( pageMsg, pageModel ) of
            ( RobotMsg msg, RobotModel model ) -> Page.Robot.update msg model baseModel.apiKey |> toRoot RobotModel RobotMsg
            ( EnterMsg msg, EnterModel model ) -> Page.Enter.update msg model baseModel.routeKey baseModel.apiKey |> toAuth EnterModel EnterMsg
            ( UserMsg msg, UserModel model ) -> Page.User.update msg model baseModel.apiKey |> toRoot UserModel UserMsg
            ( _, _ ) -> ( baseModel, pageModel, Cmd.none )
    in
    ( (newBaseModel, newPageModel), newCmd )


-- SUBSCRIPTIONS

subscriptions : Model -> Sub Msg
subscriptions ( baseModel, pageModel ) =
    let toRoot pageMsg sub =
            sub |> Sub.map pageMsg |> Sub.map Page
    in
    case pageModel of
        RobotModel model -> Page.Robot.subscriptions model |> toRoot RobotMsg
        _ -> Sub.none


-- VIEW

barePage : String -> ( String, Html Msg, Html Msg )
barePage message = ( message, div [] [], div [] [text message] )

view : Model -> Browser.Document Msg
view ( baseModel, pageModel ) =
    let toRoot pageMsg ( title, header, body ) =
            ( title
            , header |> Html.map pageMsg |> Html.map Page
            , body |> Html.map pageMsg |> Html.map Page )
    in
    let ( title, header, body ) = case pageModel of
            RobotModel model -> Page.Robot.view model |> toRoot RobotMsg
            EnterModel model -> Page.Enter.view model |> toRoot EnterMsg
            UserModel model -> Page.User.view model |> toRoot UserMsg
            HomeModel model -> Page.Home.view model |> toRoot HomeMsg
            NotFound -> barePage "404"
            Loading -> barePage "Loading..."
            Error -> barePage "Something went wrong"
    in
    { title = title
    , body = [viewPage baseModel header body]
    }

viewPage : BaseModel -> Html Msg -> Html Msg -> Html Msg
viewPage baseModel header body =
    div [ class "root" ]
        [ viewHeader baseModel header
        , div [class "body"] [body]
        , viewFooter baseModel
        ]


viewHeader : BaseModel -> Html Msg -> Html Msg
viewHeader baseModel header =
    div [ class "header" ]
        [ header
        , div [] (
            [ Route.a Route.Warehouse [text "warehouse"]
            , Route.a Route.Rules [text "rules"]
            ] ++ case baseModel.auth of
                Auth.LoggedIn user -> [
                        Route.a (Route.User user.user.username) [text "profile"],
                        button [class "d-inline-block", onClick <| Auth Auth.LogOut, class "a"] [text "logout"]
                    ]
                Auth.LoggedOut -> [
                        Route.a Route.Enter [text "login / signup"]
                    ]
            )
        ]

viewFooter : BaseModel -> Html Msg
viewFooter baseModel =
    div [ class "footer" ]
        [ text "Made with <> by Chicode NFP"
        , div []
            [ a [href "https://github.com/chicode/robot-frontend/tree/master"] [text "github"]
            , text "-"
            , a [href "https://github.com/chicode/robot-rumble/issues/new"] [text "report a bug"]
            ]
        ]


