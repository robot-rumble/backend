defmodule RobotWeb.RobotController do
  use RobotWeb, :controller

  alias Robot.{Robot, Repo}

  action_fallback(RobotWeb.FallbackController)

  plug(:force_authenticated when action not in [:index, :show])

  def index(_conn, _params) do
    Robot
    |> Repo.preload(:author)
    |> Enum.map(&Robot.bare(&1, [:author]))
  end

  def show(_conn, %{"username" => username, "slug" => slug}) do
    Robot
    |> where([p], p.slug == ^slug)
    |> join(:inner, [p], a in assoc(p, :author))
    |> where([p, a], a.username == ^username)
    |> preload([p, a], author: a)
    |> Repo.one!()
  end

  def create(conn, %{"robot" => robot_params}) do
    changeset =
      conn.assigns.user
      |> Ecto.build_assoc(:robots)
      |> Robot.changeset(robot_params)

    Repo.insert(changeset) |> IO.inspect()
  end

  def update(conn, %{"id" => id, "robot" => robot_params}) do
    robot = Repo.get!(Robot, id)

    with {:ok} <- verify_ownership(conn, robot) do
      {:ok, time} = DateTime.now("Etc/UTC")
      time = DateTime.to_unix(time)
      IO.inspect(Map.merge(robot_params, %{last_edit: time}))
      changeset = Robot.changeset(robot, Map.merge(robot_params, %{"last_edit" => time}))
      Repo.update(changeset)
    end
  end

  def delete(conn, %{"id" => id}) do
    robot = Repo.get!(Robot, id)

    with {:ok} <- verify_ownership(conn, robot) do
      Repo.delete(robot) |> IO.inspect()
    end
  end

  defp verify_ownership(conn, robot) do
    robot = Repo.preload(robot, :author)

    if robot.author == conn.assigns.user do
      {:ok}
    else
      {:error, :unauthorized}
    end
  end
end
