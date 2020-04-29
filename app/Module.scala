import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import services._

/**
  * This class is a Guice module that tells Guice how to bind several
  * different types. This Guice module is created when the Play
  * application starts.

  * Play will automatically use any class called `Module` that is in
  * the root package. You can create modules in other locations by
  * adding `play.modules.enabled` settings to the `application.conf`
  * configuration file.
  */
class Module(environment: Environment, configuration: Configuration)
    extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Db]).to(classOf[Postgres])
    if (configuration.get[Boolean]("aws.runQueue")) {
      if (configuration.get[Boolean]("aws.useMockQueue"))
        bind(classOf[BattleQueue]).to(classOf[MockQueue])
      else
        bind(classOf[BattleQueue]).to(classOf[AwsQueue]).asEagerSingleton()
      bind(classOf[MatchMaker]).asEagerSingleton()
    }
  }
}
