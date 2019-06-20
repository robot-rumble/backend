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

import Page.Robot
import Api


-- MAIN

main : Program Flags Model Msg
main =
    Browser.application
        { init = init
        , view = view
        , update = update , subscriptions = subscriptions
        , onUrlChange = UrlChanged
        , onUrlRequest = LinkClicked
        }



-- MODEL

type alias Model = ( BaseModel, PageModel )

type alias BaseModel =
    { key : Nav.Key
    , flags : Flags
    , auth : Api.Auth
    }

type PageModel
    = RobotModel Page.Robot.Model
    | NotFound
    | Redirect


-- INIT


type alias Flags =
    { totalTurns: Int
    , auth : Decode.Value
    }

init : Flags -> Url.Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url key =
    let auth = case Decode.decodeValue Api.authUserDecoder flags.auth of
            Ok user ->
                Api.LoggedIn user
            Err _ ->
                Api.LoggedOut
    in
    initPageModel url ( BaseModel key flags Api.LoggedOut, Redirect )



-- UPDATE

type Msg
    = LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | Page PageMsg

type PageMsg
    = RobotMsg Page.Robot.Msg

update : Msg -> Model -> ( Model, Cmd Msg )
update rootMsg rootModel =
    case rootMsg of
        LinkClicked urlRequest ->
            case urlRequest of
                Browser.Internal url ->
                    ( rootModel, Nav.pushUrl (first rootModel).key (Url.toString url) )

                Browser.External href ->
                    ( rootModel, Nav.load href )

        UrlChanged url ->
            initPageModel url rootModel

        Page pageMsg ->
            updatePageModel pageMsg rootModel

initPageModel : Url.Url -> Model -> ( Model, Cmd Msg )
initPageModel url ( baseModel, pageModel ) =
    let toRoot newPageModel newPageMsg ( newModel, newCmd ) =
            ( newPageModel newModel, newCmd |> Cmd.map newPageMsg |> Cmd.map Page )
    in
    let ( newPageModel, newCmd ) = case Route.parse url of
            Nothing -> ( NotFound, Cmd.none )
            Just route -> case route of
                Route.Robot user robot -> Page.Robot.init user robot baseModel.flags.totalTurns |> toRoot RobotModel RobotMsg
                Route.Home -> Page.Robot.init "" "" baseModel.flags.totalTurns |> toRoot RobotModel RobotMsg
                _ -> ( NotFound, Cmd.none )
    in
    ( (baseModel, newPageModel), newCmd )

updatePageModel : PageMsg -> Model -> ( Model, Cmd Msg )
updatePageModel pageMsg ( baseModel, pageModel ) =
    let toRoot newPageModel newPageMsg ( newModel, newCmd ) =
            ( newPageModel newModel, newCmd |> Cmd.map newPageMsg |> Cmd.map Page )
    in
    let ( newPageModel, newCmd ) = case ( pageMsg, pageModel ) of
            ( RobotMsg msg, RobotModel model ) -> Page.Robot.update msg model |> toRoot RobotModel RobotMsg
            ( _, _ ) -> ( pageModel, Cmd.none )
    in
    ( (baseModel, newPageModel), newCmd )


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

view : Model -> Browser.Document Msg
view ( baseModel, pageModel ) =
    let toRoot pageMsg ( title, header, body ) =
            ( title
            , header |> Html.map pageMsg |> Html.map Page
            , body |> Html.map pageMsg |> Html.map Page )
    in
    let ( title, header, body ) = case pageModel of
            RobotModel model -> Page.Robot.view model baseModel.auth |> toRoot RobotMsg
            _ -> ( "", div [] [], div [] [] )
    in
    { title = title
    , body = [viewPage baseModel header body]
    }

viewPage : BaseModel -> Html Msg -> Html Msg -> Html Msg
viewPage baseModel header body =
    div []
        [ viewHeader baseModel header
        , body
        , viewFooter baseModel
        ]


viewHeader : BaseModel -> Html Msg -> Html Msg
viewHeader baseModel header =
    div [ class "d-flex" ]
        [ header
        , div [] (
            [ Route.a Route.Warehouse [text "warehouse"]
            , Route.a Route.Rules [text "rules"]
            ] ++ case baseModel.auth of
                Api.LoggedIn user -> [
                        Route.a Route.Profile [text "profile"]
                    ]
                Api.LoggedOut -> [
                        Route.a Route.Enter [text "login / signup"]
                    ]
            )
        ]

viewFooter : BaseModel -> Html Msg
viewFooter baseModel =
    div [ class "d-flex" ]
        [ text "Made with <> by Chicode NFP"
        , div []
            [ a [] [text "github"]
            , text "-"
            , a [] [text "report a bug"]
            ]
        ]


