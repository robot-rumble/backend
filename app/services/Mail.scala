package services

import courier._
import play.api.Configuration

import javax.inject.Inject
import javax.mail.internet.InternetAddress
import scala.concurrent.{ExecutionContext, Future}

trait Mail {
  def mail(to: String, subject: String, body: String): Future[Unit]
}

class GMail @Inject()(config: Configuration)(implicit ec: ExecutionContext) extends Mail {
  val mailer = Mailer("mail.gmx.com", 587)
    .auth(true)
    .as(config.get[String]("email.email"), config.get[String]("email.password"))
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
