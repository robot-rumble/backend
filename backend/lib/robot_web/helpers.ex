defmodule RobotWeb.Helpers do
  import Plug.Conn
  import Phoenix.Controller

  def send_json(conn, status, json) do
    conn
    |> put_status(status)
    |> json(json)
  end

  def send_status(conn, status) do
    conn
    |> send_resp(status, "")
  end

  defmodule MapExtras do
    def get_and_update!(map, key, update) do
      map
      |> Map.get_and_update!(key, &{&1, update.(&1)})
      |> elem(1)
    end
  end
end
