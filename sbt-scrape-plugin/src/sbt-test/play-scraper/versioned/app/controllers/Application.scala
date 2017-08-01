package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._

class Application @Inject() ( assets:        Assets
                            , environment:   play.api.Environment
                            , configuration: play.api.Configuration
                            ) extends InjectedController {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def favicon: Action[AnyContent] =
    assets.versioned(path="/public/images", file = "favicon.ico")

}
