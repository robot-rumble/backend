package filters

import javax.inject._
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
 * This is a simple filter that adds a header to all requests. It's
 * added to the application's list of filters by the
 * [[Filters]] class.
 *
 * @param ec This class is needed to execute code asynchronously.
 *           It is used below by the `map` method.
 */
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