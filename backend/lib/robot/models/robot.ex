defmodule Robot.Robot do
  use Ecto.Schema
  import Ecto.Changeset
  import Robot.Helpers

  @base_properties [:id, :name, :slug, :code, :last_edit]

  @derive {Jason.Encoder, only: [:author | @base_properties]}
  schema "robots" do
    field(:name, :string)
    field(:slug, :string)
    field(:code, :string)
    field(:description, :string)
    field(:last_edit, :integer)
    belongs_to(:author, Robot.User, foreign_key: :user_id)

    timestamps()
  end

  def bare(robot, additional \\ []) do
    robot
    |> Map.from_struct()
    |> Map.take(@base_properties ++ additional)
  end

  def changeset(robot, attrs) do
    robot
    |> Robot.Repo.preload([:author])
    |> cast(attrs, [:name, :code, :description, :last_edit])
    |> validate_required([:name])
    |> default(:code, "")
    |> default(:last_edit, 0)
    |> validate_length(:name, min: 1, max: 60)
    |> custom_change(:name, :slug, false, &slugify/1)
    |> assoc_constraint(:author)
  end
end
