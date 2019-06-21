defmodule Robot.Helpers do
  import Ecto.Changeset

  def custom_change(changeset, field, destination, change_func) do
    case changeset do
      %Ecto.Changeset{valid?: true, changes: changes} ->
        put_change(changeset, destination, change_func.(Map.get(changes, field)))

      _ ->
        changeset
    end
  end

  def custom_change_all(changeset, destination, change_func) do
    case changeset do
      %Ecto.Changeset{valid?: true} ->
        put_change(changeset, destination, change_func.(changeset))

      _ ->
        changeset
    end
  end

  def default(changeset, field, default_val) do
    custom_change(changeset, field, field, fn val ->
      if is_nil(val) do
        default_val
      else
        val
      end
    end)
  end

  def custom_validation(changeset, field, validation_func, error) do
    validate_change(changeset, field, fn _, val ->
      if validation_func.(val), do: [], else: [{field, error}]
    end)
  end

  def valid_url?(url) do
    ~r/^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/
    |> Regex.match?(url)
  end

  def valid_email?(email) do
    ~r/^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
    |> Regex.match?(email)
  end

  def slugify(str) do
    str
    |> String.downcase()
    |> String.replace(~r/[^\w-]+/u, "-")
  end
end
