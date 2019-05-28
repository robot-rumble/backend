defmodule RobotWeb.FallbackController do
  use RobotWeb, :controller

  def call(conn, {:error, %Ecto.Changeset{} = changeset}) do
    errors = Ecto.Changeset.traverse_errors(changeset, &RobotWeb.ErrorHelpers.translate_error/1)

    send_json(conn, :unprocessable_entity, %{errors: errors})
  end

  def call(conn, {:error, status}) when is_atom(status) do
    send_status(conn, status)
  end

  def call(conn, {:error, error}) do
    send_json(conn, :bad_request, %{error: error})
  end

  def call(conn, {:ok, data}) do
    send_json(conn, :ok, data)
  end

  def call(conn, {:ok}) do
    send_status(conn, :ok)
  end

  def call(conn, data) do
    send_json(conn, :ok, data)
  end
end
