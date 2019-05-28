defmodule Robot.Guardian do
  use Guardian, otp_app: :robot

  def subject_for_token(user, _claims) do
    {:ok, to_string(user.id)}
  end

  def resource_from_claims(claims) do
    {:ok, Robot.Repo.get!(Robot.User, claims["sub"])}
  end
end
