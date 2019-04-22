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

import Decode as CustomDecode
import Json.Decode as Decode

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
    , output : Maybe CustomDecode.Output
    , error : Maybe String
    }


init : () -> Url.Url -> Nav.Key -> ( Model, Cmd Msg )
init flags url key =
    (Model key url Nothing Nothing, Cmd.none )



-- UPDATE

port startEval : String -> Cmd msg

type Msg
    = LinkClicked Browser.UrlRequest
    | UrlChanged Url.Url
    | GotOutput Decode.Value
    | Run

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

        GotOutput output ->
          case CustomDecode.decodeOutput output of
            Ok data ->
              ( { model | output = Just data }, Cmd.none )
            Err error ->
              let _ = Debug.log "Error" <| Debug.toString error in
              ( model, Cmd.none )


        Run ->
            ( model, startEval """
	function main (input) {
		console.log(input)
		return { actions: {} }
	}
            """ )



-- SUBSCRIPTIONS

port getOutput : (Decode.Value -> msg) -> Sub msg

subscriptions : Model -> Sub Msg
subscriptions _ =
   getOutput GotOutput

-- VIEW


view : Model -> Browser.Document Msg
view model =
    { title = "Copala"
    , body =
        [button [onClick Run] [text "run"]
        , case model.output of
            Just output -> div [] [text output.winner]
            _ -> div [] []
        ]
    }

