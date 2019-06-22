defmodule Robot.Repo.Migrations.AddUsersRobots do
  use Ecto.Migration

  def change do
    create table(:users) do
      add(:email, :string)
      add(:password_hash, :string)
      add(:username, :string)

      timestamps()
    end

    create(unique_index(:users, [:username]))

    create table(:robots) do
      add(:name, :string)
      add(:slug, :string)
      add(:description, :text)
      add(:code, :text)
      add(:last_edit, :integer)
      add(:user_id, references(:users))

      timestamps()
    end
  end
end
