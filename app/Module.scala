import com.google.inject.AbstractModule
import matchmaking._
import play.api.{Configuration, Environment, Logger}
import services.{Database, GMail, Mail, Postgres}

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.

  * Play will automatically use any class called `Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class Module(_env: Environment, config: Configuration) extends AbstractModule {
  val logger: Logger = Logger(this.getClass)
  override def configure(): Unit = {
    logger.debug("Starting Dependency Injection...")
    bind(classOf[Database]).to(classOf[Postgres]).asEagerSingleton()
    bind(classOf[Mail]).to(classOf[GMail])
    if (config.get[Boolean]("queue.enabled")) {
      if (config.get[Boolean]("queue.useMock")) {
        logger.debug("Launching Mock Queue...")
        bind(classOf[BattleQueue]).to(classOf[MockQueue])
      } else {
        logger.debug("Launching AWS Queue...")
        bind(classOf[BattleQueue]).to(classOf[AwsQueue]).asEagerSingleton()
      }
      logger.debug("Launching MatchMaker...")
      bind(classOf[MatchMaker]).asEagerSingleton()
    }
  }
}
