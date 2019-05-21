defmodule Robot.Repo do
  use Ecto.Repo,
    otp_app: :robot,
    adapter: Ecto.Adapters.Postgres
end
