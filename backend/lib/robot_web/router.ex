defmodule RobotWeb.Router do
  use RobotWeb, :router

  pipeline :api do
    plug(:accepts, ["json"])
  end

  scope "/api", RobotWeb do
    pipe_through(:api)
  end
end
