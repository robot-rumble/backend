module Api exposing (..)

import Url
import Url.Builder exposing (relative, crossOrigin)
import Json.Decode exposing (..)
import Json.Decode.Pipeline exposing (required, custom)
import Json.Encode as Encode
import Http

type alias Error = Http.Error

type alias Endpoint val = (List String, Decoder val)

type alias Key = String

type Request val bodyVal
    = Get (Endpoint val)
    | Post (Endpoint val) Encode.Value


makeRequest : Request val body -> (Result Http.Error val -> msg) -> Key -> Cmd msg
makeRequest request msg key =
    case request of
        Get (url, decoder) ->
            Http.get {
                url = crossOrigin key url [],
                expect = Http.expectJson msg decoder
            }
        Post (url, decoder) body ->
            Http.post {
                url = crossOrigin key url [],
                expect = Http.expectJson msg decoder,
                body = Http.jsonBody body
            }


-- USER

type alias User =
    { id : Int
    , username : String
    , email : String
    , robots : List Robot
    }

userDecoder = succeed User
    |> required "id" int
    |> required "username" string
    |> required "email" string
    |> required "robots" (list robotDecoder)

allUsers = Get (["users"], list userDecoder) |> makeRequest
getUser user = Get (["users", user], userDecoder) |> makeRequest


-- ROBOT

type alias RobotAuthor =
    { id : Int
    , username : String
    , email : String
    }

robotAuthorDecoder = succeed RobotAuthor
    |> required "id" int
    |> required "username" string
    |> required "email" string


type alias Robot =
    { id : Int
    , name : String
    , code : String
    , slug : String
    , last_edit : Int
    , author : RobotAuthor
    }

robotDecoder = succeed Robot
    |> required "id" int
    |> required "name" string
    |> required "code" string
    |> required "slug" string
    |> required "last_edit" int
    |> required "author" robotAuthorDecoder


type alias CreateRobotBody =
    { jwt : String
    , name : String
    }

createRobotEncoder body = Encode.object [
        ("jwt", Encode.string body.jwt),
        ("robot", Encode.object [
            ("name", Encode.string body.name)
        ])
    ]

type alias UpdateRobotBody =
    { jwt : String
    , code : String
    }

updateRobotEncoder body = Encode.object [
        ("jwt", Encode.string body.jwt),
        ("robot", Encode.object [
            ("code", Encode.string body.code)
        ])
    ]

getRobot user robot = Get (["robots", user, robot], robotDecoder) |> makeRequest

createRobot body =
    Post (["robots"], robotDecoder) (createRobotEncoder body) |> makeRequest

updateRobot id body =
    Post (["robots", String.fromInt id, "update"], robotDecoder) (updateRobotEncoder body) |> makeRequest


-- AUTH

type alias BareUser =
    { id : Int
    , username : String
    , email : String
    }

bareUserDecoder = succeed BareUser
    |> required "id" int
    |> required "username" string
    |> required "email" string

bareUserEncoder body = Encode.object [
                ("id", Encode.int body.id),
                ("username", Encode.string body.username),
                ("email", Encode.string body.email)
        ]


type alias AuthUser = { jwt : String, user : BareUser }

authUserDecoder = succeed AuthUser
    |> required "jwt" string
    |> required "user" bareUserDecoder

authUserEncoder body = Encode.object [
        ("jwt", Encode.string body.jwt ),
        ("user", bareUserEncoder body.user)
    ]


type alias SignUpBody = {
        username : String,
        password : String,
        email : String
    }

signUpEncoder body = Encode.object [
        ("user", Encode.object [
            ("username", Encode.string body.username),
            ("password", Encode.string body.password),
            ("email", Encode.string body.email)
        ])
    ]

signUp body =
    Post ( ["users"], bareUserDecoder ) (signUpEncoder body) |> makeRequest


type alias LogInBody = {
        username : String,
        password : String
    }

logInEncoder body = Encode.object [
        ("session", Encode.object [
            ("username", Encode.string body.username),
            ("password", Encode.string body.password)
        ])
    ]

logIn body =
    Post (["sessions"], authUserDecoder ) (logInEncoder body) |> makeRequest
