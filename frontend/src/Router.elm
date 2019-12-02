module Router exposing (..)
import Url.Parser exposing (..)
import Url

type Route
    = Robot String String
    | Profile String
    | Home

routeParser : Parser (Route -> a) a
routeParser =
    oneOf
        [ map Robot (string </> string)
        , map Profile (string)
        , map Home top
        ]

parse : Url.Url -> Maybe Route
parse = Url.Parser.parse routeParser
