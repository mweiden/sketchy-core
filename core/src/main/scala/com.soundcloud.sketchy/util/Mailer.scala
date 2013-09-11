package com.soundcloud.sketchy.util

import java.util.Properties._
import javax.mail._
import javax.mail.internet._

/**
 * Send emails via SMTP
 */
class Mailer(subject: String, recipient: String, sender: String) {
  val properties = System.getProperties
  properties.put("mail.smtp.host", "localhost")
  properties.put("mail.smtp.timeout", "10000")
  properties.put("mail.smtp.connectiontimeout", "10000")

  val session = Session.getDefaultInstance(properties)

  def send(body: String) = Transport.send(build(body))

  private def build(body: String): MimeMessage = {
    val message = new MimeMessage(session)
    message.setFrom(new InternetAddress(sender))
    message.setRecipients(Message.RecipientType.TO, recipient)
    message.setSubject(subject)
    message.setText(body)
    message
  }
}

