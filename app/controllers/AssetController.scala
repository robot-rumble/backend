package controllers

import javax.inject._
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class AssetController @Inject()(cc: ControllerComponents, assets: Assets)(implicit ec : ExecutionContext) extends AbstractController(cc) {
  def wasm(file : String): Action[AnyContent] = Action.async { request =>
    assets.at(file + ".wasm")(request).map(_.as("application/wasm"))
  }
}
