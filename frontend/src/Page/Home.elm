module Page.Home exposing (Model, Msg, init, view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)

import Auth
import Api
import Route

-- MODEL

type alias Model =
    {}

init : ( Model, Cmd msg )
init =
    ( Model, Cmd.none )


-- UPDATE

type Msg
    = Nothing

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model = ( model, Cmd.none )

-- VIEW


view : Model -> ( String, Html msg, Html msg )
view model =
    ( "Robot Rumble", div [] [text "Robot Rumble"], viewBody model )


viewBody : Model -> Html msg
viewBody model =
    div []
        [ p [] [text "Welcome to Robot Rumble!"]
        , p [] [text "To get started, sign in or view the ", Route.a Route.Demo [text "demo"], text "."]
        ]
