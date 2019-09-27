package nl.egulden.discordbot.controllers

import javax.inject._
import play.api.Logger
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents)
                              (implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  val logger = Logger(getClass)

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
}
