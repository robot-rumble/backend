defmodule Robot.User do
  use Ecto.Schema
  import Ecto.Changeset
  import Robot.Helpers

  @base_properties [:id, :username, :email]

  @derive {Jason.Encoder, only: @base_properties}
  schema "users" do
    field(:username, :string)
    field(:email, :string)
    field(:password, :string, virtual: true)
    field(:password_hash, :string)
    has_many(:robots, Robot.Robot)

    timestamps()
  end

  def bare(user, additional \\ []) do
    user
    |> Map.from_struct()
    |> Map.take(@base_properties ++ additional)
  end

  def create_changeset(user, attrs) do
    user
    |> cast(attrs, [:username, :password, :email])
    |> validate_required([:username, :password, :email])
    |> validate_length(:username, min: 1, max: 20)
    |> unique_constraint(:username)
    |> changeset(attrs)
  end

  def changeset(user, attrs) do
    user
    |> cast(attrs, [:email, :password])
    |> custom_validation(:email, &valid_email?/1, "Invalid email address")
    |> validate_length(:password, min: 6, max: 50)
    |> custom_change(:password, :password_hash, &Comeonin.Bcrypt.hashpwsalt/1)
  end
end
