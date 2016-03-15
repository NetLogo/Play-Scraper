package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._

class Application @Inject() (environment: play.api.Environment, configuration: play.api.Configuration) extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

}
