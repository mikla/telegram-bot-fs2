package ru.pavkin.telegram.api

import cats.effect.Sync
import cats.implicits._
import fs2.Stream
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}
import org.typelevel.log4cats.Logger
import ru.pavkin.telegram.api.dto.{BotResponse, BotUpdate}

/**
  * Simplified bot api algebra that exposes only APIs required for this project
  *
  * S is the streaming effect, see https://typelevel.org/blog/2018/05/09/tagless-final-streaming.html
  *
  * For the full API reference see https://core.telegram.org/bots/api
  */
trait BotAPI[F[_], S[_]] {
  /**
    * Send a message to specified chat
    */
  def sendMessage(chatId: ChatId, message: String): F[Unit]

  /**
    * Stream all updated for this bot using long polling. `S[_]` is the streaming effect.
    *
    * @param fromOffset offset of the fist message to start polling from
    */
  def pollUpdates(fromOffset: Offset): S[BotUpdate]
}

trait StreamingBotAPI[F[_]] extends BotAPI[F, Stream[F, *]]

/**
  * Single bot API instance with http4s client.
  * Requires an implicit decoder for incoming bot updates.
  *
  * @param token  bot api token
  * @param client http client algebra
  * @param logger logger algebra
  */
class Http4SBotAPI[F[_]](
  token: String,
  client: Client[F],
  logger: Logger[F])(
  implicit
  F: Sync[F],
  D: EntityDecoder[F, BotResponse[List[BotUpdate]]],
  U: EntityDecoder[F, Unit]) extends StreamingBotAPI[F] {

  private val botApiUri: Uri = Uri.uri("https://api.telegram.org") / s"bot$token"

  def sendMessage(chatId: ChatId, message: String): F[Unit] = {

    // safely build a uri to query
    val uri = botApiUri / "sendMessage" =? Map(
      "chat_id" -> List(chatId.toString),
      "parse_mode" -> List("Markdown"),
      "text" -> List(message)
    )

    client.expect[Unit](uri) // run the http request and ignore the result body.
  }

  def pollUpdates(fromOffset: Offset): Stream[F, BotUpdate] =
    Stream(()).repeat.covary[F]
      .evalMapAccumulate(fromOffset) { case (offset, _) => requestUpdates(offset) }
      .flatMap { case (_, response) => Stream.emits(response.result) }

  private def requestUpdates(offset: Offset): F[(Offset, BotResponse[List[BotUpdate]])] = {

    val uri = botApiUri / "getUpdates" =? Map(
      "offset" -> List((offset + 1).toString),
      "timeout" -> List("0.5"), // timeout to throttle the polling
      "allowed_updates" -> List("""["message"]""")
    )

    client.expect[BotResponse[List[BotUpdate]]](uri)
      .map(response => (lastOffset(response).getOrElse(offset), response))
      .recoverWith {
        case ex => logger.error(ex)("Failed to poll updates").as(offset -> BotResponse(ok = true, Nil))
      }
  }

  // just get the maximum id out of all received updates
  private def lastOffset(response: BotResponse[List[BotUpdate]]): Option[Offset] =
    response.result match {
      case Nil => None
      case nonEmpty => Some(nonEmpty.maxBy(_.update_id).update_id)
    }
}
