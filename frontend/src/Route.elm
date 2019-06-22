module Route exposing (Route(..), parse, a, push, Key)

import Url.Parser exposing (..)
import Url.Builder
import Url
import Html
import Html.Attributes
import Browser.Navigation as Nav

type alias Key = Nav.Key

type Route
    = Robot String String
    | User String
    | Home
    | Warehouse
    | Rules
    | Enter
    | Demo

routeParser : Parser (Route -> a) a
routeParser =
    oneOf
        [ map Warehouse (s "warehouse")
        , map Rules (s "rules")
        , map Enter (s "enter")
        , map Demo (s "demo")
        , map Robot (string </> string)
        , map User (string)
        , map Home top
        ]

parse : Url.Url -> Maybe Route
parse = Url.Parser.parse routeParser

toString : Route -> String
toString route =
    let pieces = case route of
            Robot user robot -> [user, robot]
            User user -> [user]
            Warehouse -> ["warehouse"]
            Rules -> ["rules"]
            Enter -> ["enter"]
            Demo -> ["demo"]
            Home -> []
    in
    Url.Builder.absolute pieces []

a : Route -> List (Html.Html msg) -> Html.Html msg
a route =
    Html.a [Html.Attributes.href <| toString route]

push : Nav.Key -> Route -> Cmd msg
push key route =
    Nav.pushUrl key (toString route)
