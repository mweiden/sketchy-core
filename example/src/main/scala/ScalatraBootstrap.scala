import javax.servlet.ServletContext

import com.soundcloud.sketchy.ingester.HTTPIngester
import org.slf4j.{LoggerFactory,Logger}
import org.scalatra._


class ScalatraBootstrap extends LifeCycle {


   val loggerName = this.getClass.getName
   lazy val log = LoggerFactory.getLogger(loggerName)

  override def init(context: ServletContext) {
    log.info(msg("Starting the ingestion servlets."))

    HTTPIngester.servlets.map(ingester => context.mount(ingester, "/*"))

    log.info(msg("Api ingestion servlets mounted."))
  }

  private def msg(str: String) = "[Scalatra] " + str

}

