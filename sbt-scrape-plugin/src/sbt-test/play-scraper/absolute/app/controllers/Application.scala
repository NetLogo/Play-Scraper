package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._

class Application @Inject() (
  environment: Environment,
  configuration: Configuration,
  val controllerComponents: ControllerComponents
) extends BaseController {

  def index = Action({
    implicit request: Request[AnyContent] =>
      Ok(views.html.index("Your new application is ready."))
  })

}
