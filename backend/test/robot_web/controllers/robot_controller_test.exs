defmodule RobotWeb.RobotControllerTest do
  use RobotWeb.ConnCase, async: true

  alias Robot.{User, Robot, Repo}

  @user_attrs %{
    username: "root",
    password: "password123",
    email: "root@root.com"
  }

  @robot_attrs %{
    name: "robot",
    code: "abc"
  }

  defp create_user(_) do
    {:ok, user} =
      %User{}
      |> User.create_changeset(@user_attrs)
      |> Repo.insert()

    {:ok, user: user}
  end

  defp create_robot(_) do
    {:ok, robot} =
      User
      |> Repo.get_by(username: "root")
      |> Ecto.build_assoc(:robots)
      |> Robot.changeset(@robot_attrs)
      |> Repo.insert()

    robot_resp =
      robot
      |> Robot.bare()
      |> string_map()

    {:ok, robot: robot, robot_resp: robot_resp}
  end

  setup [:create_user, :create_robot]

  test "GET /", %{conn: conn, user: user, robot: robot, robot_resp: robot_resp} do
    resp =
      conn
      |> get(Routes.robot_path(conn, :show, user.username, robot.slug))
      |> json_response(200)

    assert resp == robot_resp
  end
end
