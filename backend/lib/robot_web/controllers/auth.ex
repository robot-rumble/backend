defmodule RobotWeb.Auth do
  import Plug.Conn
  import RobotWeb.Helpers

  def authenticate(username, password) do
    user = Robot.Repo.get_by(Robot.User, username: username)

    cond do
      user && Comeonin.Bcrypt.checkpw(password, user.password_hash) ->
        {:ok, user}

      user ->
        {:error, :unauthorized}

      true ->
        Comeonin.Bcrypt.dummy_checkpw()
        {:error, :not_found}
    end
  end

  def process_token(conn, _opts) do
    with %{"jwt" => jwt} <- conn.params,
         {:ok, claims} <- Robot.Guardian.decode_and_verify(jwt) do
      {:ok, user} = Robot.Guardian.resource_from_claims(claims)
      assign(conn, :user, user)
    else
      _ -> assign(conn, :user, nil)
    end
  end

  def force_authenticated(conn, _opts) do
    if is_nil(conn.assigns.user) do
      conn
      |> send_json(:bad_request, %{error: "Missing jwt token."})
      |> halt
    else
      conn
    end
  end
end
