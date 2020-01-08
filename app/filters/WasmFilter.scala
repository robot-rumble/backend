package filters

import javax.inject._
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class WasmFilter @Inject()(implicit ec: ExecutionContext) extends EssentialFilter {
  override def apply(next: EssentialAction) = EssentialAction { request =>
    next(request).map { result =>
      if (request.headers.get("uri").contains(".wasm"))
        result.withHeaders("ContentType" -> "application/wasm")
      else result
    }
  }
}