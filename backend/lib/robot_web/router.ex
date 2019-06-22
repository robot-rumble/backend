defmodule RobotWeb.Router do
  use RobotWeb, :router
  import RobotWeb.Auth, only: [process_token: 2]

  pipeline :api do
    plug(:accepts, ["json"])
    plug(:process_token)
  end

  scope "/api", RobotWeb do
    pipe_through(:api)

    scope "/v1" do
      scope "/users" do
        get("/", UserController, :index)
        post("/me", UserController, :me)
        get("/:username", UserController, :show)
        post("/", UserController, :create)
        # patch("/:id", UserController, :update)
        # delete("/:id", UserController, :delete)
      end

      scope "/robots" do
        get("/", RobotController, :index)
        get("/:username/:slug", RobotController, :show)
        post("/", RobotController, :create)
        post("/:id/update", RobotController, :update)
        post("/:id/delete", RobotController, :delete)
      end

      post("/sessions", SessionController, :create)
    end
  end
end
