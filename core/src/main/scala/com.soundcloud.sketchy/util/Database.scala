package com.soundcloud.sketchy.util

import java.text.SimpleDateFormat
import java.util.Date

import com.soundcloud.sketchy.monitoring.Instrumented
import org.apache.commons.dbcp.BasicDataSource
import org.slf4j.{LoggerFactory,Logger}

import scala.slick.driver.MySQLDriver.backend.{Database => SlickDatabase}


class Database(cfgs: List[DatabaseCfg]) extends Instrumented  {

  val name = cfgs.head.name
  def metricsTypeName = cfgs.head.name
  def metricsSubtypeName: Option[String] = None

  val attemptsPerHost = 0

  val masters  = cfgs.filter(_.readOnly == false).map(_.register)
  val slaves   = cfgs.filter(_.readOnly != false).map(_.register)

  val loggerName = this.getClass.getName
  lazy val logger = LoggerFactory.getLogger(loggerName)


  def withFailover[T](
    operation: String,
    writeOp: Boolean,
    isQuiet: Boolean = false)(
    dbOperation: => T): Option[T] = {

    var result: Option[T] = None

    val dbs = scala.util.Random.shuffle(
      if (writeOp) masters else slaves ++ masters)

    if (!dbs.isEmpty) {
      val dbIterator = dbs.iterator

      while(dbIterator.hasNext && result.isEmpty) {
        result = try {
            val selectedDb = dbIterator.next
            if (Database.circuitBreaker.isActive(selectedDb)) {
              selectedDb withDynSession {
                Some(dbOperation)
              }
            } else {
              None
            }
          } catch {
            case e: Throwable => {
              if (!isQuiet) {
                Exceptions.report(e)
                logger.error("could not perform %s operation: %s"
                  .format(if (writeOp) "write" else "read", operation),e)
              }
              None
            }
          }
        val status = if (result.isDefined) "success" else "failure"
        meter(operation, status)
      }
    }
    result
  }

  /**
   * management
   */
  def active = cfgs.head.active
  def idle = cfgs.head.idle

  // metrics setup
  private val counter = prometheusCounter("db", List("name", "operation", "status"))

  private def meter(operation: String, status: String) {
    counter
      .labels(metricsTypeName, operation, status)
      .inc()
  }
}

/**
 * Some utils
 */
object Database {
  val dateFormat = "yyyy-MM-dd HH:mm:ss"

  def date(d: Date): String = {
    val dateFormatter = new SimpleDateFormat(dateFormat)
    dateFormatter.format(d)
  }

  val circuitBreaker = new CircuitBreaker
}

/**
 * Loads production or test drivers
 */
trait Driver {
  val name: String
  def uri(cfg: DatabaseCfg): String
}

class MysqlDriver extends Driver {
  val name = "com.mysql.jdbc.Driver"

  def uri(cfg: DatabaseCfg): String = {
    "jdbc:mysql://" + cfg.host + ":" + cfg.port + "/" + cfg.db
  }
}

/**
 * Connection pooling
 * ------------------
 *
 * Connect provides a database connection pool via apache commons dbcp.
 * org.apache.commons.dbcp.PoolingDriver is registered as the main JDBC
 * driver. The driver is used to register an instance of
 * GenericObjectPool[Connection], as provided by commons.pool. The actual
 * connection creation is delegated to a DriverManagerConnectionFactory. This
 * class loads and parameterizes the underlying mysql ab jdbc driver,
 * com.mysql.jdbc.Driver.
 *
 * Failover
 * --------
 *
 * Since failover is not provided at the level of the
 * DriverManagerConnectionFactory failover is faked by having multiple
 * database configurations and switching them out upon an error.
 */
case class DatabaseCfg(
  name: String,
  user: String,
  password: String,
  host: String,
  db: String,
  driver: Driver = new MysqlDriver(),
  port: Int = 3306,
  maxActive: Int = 20,
  maxIdle: Int = 1,
  maxWait: Int = 1000,
  validationQuery: String = "SELECT 1 /* skechy-core-validation */",
  readOnly: Boolean = true,
  autoCommit: Boolean = true) {

  val ds = new BasicDataSource

  def register: SlickDatabase = {
    ds.setDriverClassName(driver.name)
    ds.setUrl(uri)
    ds.setUsername(user)
    ds.setPassword(password)

    ds.setMaxActive(maxActive)
    ds.setMaxIdle(maxIdle)
    ds.setMaxWait(maxWait)
    ds.setDefaultAutoCommit(autoCommit)
    ds.setValidationQuery(validationQuery)
    ds.setDefaultReadOnly(readOnly)

    scala.slick.driver.MySQLDriver.backend.Database.forDataSource(ds)
  }

  val uri = driver.uri(this)
  def active = ds.getNumActive()
  def idle = ds.getNumIdle()

}

