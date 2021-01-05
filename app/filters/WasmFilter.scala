package filters

import play.api.mvc._

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class WasmFilter @Inject()(implicit ec: ExecutionContext) extends EssentialFilter {
  override def apply(next: EssentialAction): EssentialAction = EssentialAction { request =>
    next(request).map { result =>
      if (request.headers
            .get("Raw-Request-URI")
            .exists(_.contains(".wasm"))) {
        result.as("application/wasm")
      } else result
    }
  }
}
