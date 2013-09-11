import org.scalatra._

import com.soundcloud.sketchy.util.Logging
import com.soundcloud.sketchy.ingester.HTTPIngester

import javax.servlet.ServletContext


class ScalatraBootstrap extends LifeCycle with Logging {

  override def init(context: ServletContext) {
    log.info(msg("Starting the ingestion servlets."))

    HTTPIngester.servlets.map(ingester => context.mount(ingester, "/*"))

    log.info(msg("Api ingestion servlets mounted."))
  }

  private def msg(str: String) = "[Scalatra] " + str

}

