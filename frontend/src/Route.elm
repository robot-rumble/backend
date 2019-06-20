module Route exposing (Route(..), parse, a)

import Url.Parser exposing (..)
import Url.Builder
import Url
import Html
import Html.Attributes

type Route
    = Robot String String
    | Home
    | Warehouse
    | Profile
    | Rules
    | Enter

routeParser : Parser (Route -> a) a
routeParser =
    oneOf
        [ map Robot (string </> string)
        , map Warehouse (s "warehouse")
        , map Profile (s "profile")
        , map Rules (s "rules")
        , map Enter (s "enter")
        , map Home top
        ]

parse : Url.Url -> Maybe Route
parse = Url.Parser.parse routeParser

toString : Route -> String
toString route =
    let pieces = case route of
            Robot user robot -> [user, robot]
            Warehouse -> ["warehouse"]
            Profile -> ["profile"]
            Rules -> ["rules"]
            Enter -> ["enter"]
            Home -> []
    in
    Url.Builder.absolute pieces []

a : Route -> List (Html.Html msg) -> Html.Html msg
a route =
    Html.a [Html.Attributes.href <| toString route]
