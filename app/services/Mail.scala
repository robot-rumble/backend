package services

import courier._
import play.api.Configuration

import javax.inject.Inject
import javax.mail.internet.InternetAddress
import scala.concurrent.{ExecutionContext, Future}

trait Mail {
  def mail(to: String, subject: String, body: String): Future[Unit]
}

class SES @Inject()(config: Configuration)(implicit ec: ExecutionContext) extends Mail {
  private val mailer = Mailer(config.get[String]("email.host"), config.get[Int]("email.port"))
    .auth(true)
    .as(config.get[String]("email.smtpUsername"), config.get[String]("email.smtpPassword"))
    .startTls(true)()

  def mail(to: String, subject: String, body: String): Future[Unit] = {
    mailer(
      Envelope
        .from(new InternetAddress(config.get[String]("email.email")))
        .to(new InternetAddress(to))
        .subject(subject)
        .content(
          Text(body)
        )
    )
  }
}

class MockMail @Inject()() extends Mail {
  def mail(to: String, subject: String, body: String): Future[Unit] = {
    Future successful Unit
  }
}
