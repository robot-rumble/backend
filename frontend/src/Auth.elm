port module Auth exposing (..)

import Api
import Json.Encode as Encode
import Json.Decode as Decode

import Route

type Auth
    = LoggedIn Api.AuthUser
    | LoggedOut

type AuthCmd
    = LogIn Api.AuthUser
    | LogOut
    | None

port storeAuth : Encode.Value -> Cmd msg
port removeAuth : () -> Cmd msg

processCmd : AuthCmd -> Auth -> Route.Key -> ( Auth, Cmd msg )
processCmd authCmd auth key =
    case authCmd of
        LogIn user ->
            ( LoggedIn user, storeAuth (Api.authUserEncoder user) )

        LogOut ->
            ( LoggedOut, Cmd.batch [removeAuth (), Route.push key Route.Home] )

        None -> (auth, Cmd.none)

initAuth : Decode.Value -> Auth
initAuth auth =
    case Decode.decodeValue Api.authUserDecoder auth of
        Ok user ->
            LoggedIn user
        Err _ ->
            LoggedOut

