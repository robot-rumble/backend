module Page.Template exposing (Model, Msg, init, subscriptions, update, view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)

import Auth
import Api


-- MODEL

type alias Model =
    {}


init : ( Model, Cmd Msg )
init =
    ( Model , Cmd.none )


-- UPDATE

type Msg
    =

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of


-- VIEW


view : Model -> ( String, Html Msg, Html Msg )
view model =
    ( "", div [] [], viewBody model )


viewBody : Model -> Html Msg
viewBody model =
    div []
        []
