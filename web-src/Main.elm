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

import Json.Encode as Encode

import Json.Decode as Decode exposing (Decoder, int, string, float)
import Json.Decode.Pipeline exposing (required, optional, hardcoded)

-- MAIN


main : Program () Model Msg
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


type alias Model =
    { key : Nav.Key
    , url : Url.Url
    }


init : () -> Url.Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url key =
    (Model key url, Cmd.none )



-- UPDATE


type Msg
    = LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | GotOutput Unit

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        LinkClicked urlRequest ->
            case urlRequest of
                Browser.Internal url ->
                    ( model, Nav.pushUrl model.key (Url.toString url) )

                Browser.External href ->
                    ( model, Nav.load href )

        UrlChanged url ->
            ( { model | url = url }, Cmd.none )

        GotOutput unit ->
            ( model, Cmd.none )



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.batch
        [ getOutput (decodeUnit >> GotOutput)
        ]



-- VIEW


view : Model -> Browser.Document Msg
view model =
    { title = "Copala"
    , body =
        [div [] [text "hello!"]
        ]
    }


type alias Unit =
  { class : String
  , x : Int
  , y : Int
  , health : Int
  }


encodeUnit : Unit -> Encode.Value
encodeUnit unit =
    Encode.object
        []

unitDecoder : Decoder Unit
unitDecoder =
    Decode.succeed Unit
        |> required "class" string
        |> required "x" int
        |> required "y" int
        |> required "health" int

decodeUnit : Encode.Value -> Unit
decodeUnit = Decode.decodeValue unitDecoder >> Result.withDefault { class = "asd", x = 0, y = 0, health = 10}

port sendInput : Encode.Value -> Cmd msg
port getOutput : (Encode.Value -> msg) -> Sub msg
