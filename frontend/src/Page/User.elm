module Page.User exposing (Model, Msg, init, update, view)

import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)

import Auth
import Api


-- MODEL

type alias Model =
    { user : Api.User }


init : Api.User -> ( Model, Cmd Msg )
init user =
    ( Model user, Cmd.none )


-- UPDATE

type Msg
    = None


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        None -> (model, Cmd.none)

-- VIEW


view : Model -> Auth.Auth -> ( String, Html Msg, Html Msg )
view model auth =
    ( model.user.username ++ "'s profile", div [] [ text model.user.username ], viewBody model )


viewBody : Model -> Html Msg
viewBody model =
    div []
        [ h1 [] [ text "Robots"]
        ]
