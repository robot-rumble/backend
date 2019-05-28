defmodule RobotWeb.SessionController do
  use RobotWeb, :controller

  alias Robot.Guardian

  action_fallback(RobotWeb.FallbackController)

  def create(conn, %{"session" => %{"username" => username, "password" => password}}) do
    case RobotWeb.Auth.authenticate(username, password) do
      {:ok, user} ->
        {:ok, jwt, _full_claims} = Guardian.encode_and_sign(user)

        {:ok, %{jwt: jwt, user: user}}

      {:error, _} ->
        {:error, "Incorrect email or password!"}
    end
  end
end
