module Page.Profile exposing (Model, Msg, init, subscriptions, update, view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)

import Auth
import Api


-- MODEL

type alias Model =
    { user : Api.User
    }


init : ( Model, Cmd Msg )
init =
    ( Model , Api.user  )


-- UPDATE

type Msg
    =

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of


-- VIEW


view : Model -> Auth.Auth -> ( String, Html Msg, Html Msg )
view model auth =
    ( "", div [] [], viewBody model )


viewBody : Model -> Html Msg
viewBody model =
    div []
        []
