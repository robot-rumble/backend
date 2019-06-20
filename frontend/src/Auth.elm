port module Auth exposing (..)

import Api
import Json.Encode as Encode
import Json.Decode as Decode

type Auth
    = LoggedIn Api.AuthUser
    | LoggedOut

type AuthCmd
    = LogIn Api.AuthUser
    | None

port storeAuth : Encode.Value -> Cmd msg

processCmd : AuthCmd -> Auth -> ( Auth, Cmd msg )
processCmd authCmd auth =
    case authCmd of
        LogIn user ->
            ( LoggedIn user, storeAuth (Api.authUserEncoder user) )

        None -> (auth, Cmd.none)

initAuth : Decode.Value -> Auth
initAuth auth =
    case Decode.decodeValue Api.authUserDecoder auth of
        Ok user ->
            LoggedIn user
        Err _ ->
            LoggedOut

