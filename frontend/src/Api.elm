module Api exposing (..)

import Url
import Url.Builder exposing (relative)
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (required, custom)
import Json.Encode as Encode
import Http

type alias Error = Http.Error

type alias Endpoint val = (String, Decoder val)

type Request val bodyVal
    = Get (Endpoint val)
    | Post (Endpoint val) Encode.Value

type alias User =
    { id : String
    , username : String
    , email : String
    }

userDecoder = succeed User
    |> required "id" string
    |> required "username" string
    |> required "email" string

type alias Robot =
    { id : String
    , name : String
    , code : String
    , slug : String
    }

robotDecoder = succeed Robot
    |> required "id" string
    |> required "name" string
    |> required "code" string
    |> required "slug" string



makeRequest : Request val body -> (Result Http.Error val -> msg) -> Cmd msg
makeRequest request msg =
    case request of
        Get (url, decoder) -> Http.get {
                url = url,
                expect = Http.expectJson msg decoder
            }
        Post (url, decoder) body -> Http.post {
                url = url,
                expect = Http.expectJson msg decoder,
                body = Http.jsonBody body
            }

allUsers = Get ("/user", list userDecoder) |> makeRequest
currentUser body = Post ("/users/me", userDecoder) body |> makeRequest
user username = Get ("/" ++ username, userDecoder) |> makeRequest
createUser body = Post ("/", userDecoder) body |> makeRequest


type alias SignUpBody = {
        username : String,
        password : String,
        email : String
    }

signUpEncoder body = Encode.object [
        ("username", Encode.string body.username),
        ("password", Encode.string body.password),
        ("email", Encode.string body.email)
    ]

signUp body =
    Post ( "/users", userDecoder ) (signUpEncoder body) |> makeRequest



type alias AuthUser = { jwt : String, user : User }

authUserDecoder = succeed AuthUser
    |> required "jwt" string
    |> required "user" userDecoder


userEncoder body = Encode.object [
                ("id", Encode.string body.id),
                ("username", Encode.string body.username),
                ("email", Encode.string body.email)
        ]

authUserEncoder body = Encode.object [
        ("jwt", Encode.string body.jwt ),
        ("user", userEncoder body.user)
    ]


type alias LogInBody = {
        username : String,
        password : String
    }

logInEncoder body = Encode.object [
        ("username", Encode.string body.username),
        ("password", Encode.string body.password)
    ]


logIn body =
    Post ("/sessions", authUserDecoder ) (logInEncoder body) |> makeRequest
