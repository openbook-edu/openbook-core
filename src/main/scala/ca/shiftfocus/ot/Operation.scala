package ca.shiftfocus.ot

import play.api.libs.json._

sealed trait Operation {

  val retain: Int
  val chars: String

  implicit class Compose(val left: Operation) extends AnyVal {
    def +(right: Operation): Operation = {
      ComposedOperation(left, right)
    }
  }

  implicit class Transform(val server: Operation) extends AnyVal {

    /**
     * Given two operations performed on the client and server, transform them so that the client can perform
     * the server operation, and the server can perform the client operation, and both arrive at a consistent state.
     *
     * In times of conflict, the server always wins.
     *
     * The result is a tuple containing two operations. The left is the client operation transformed to be run on the
     * server. The right is the server's operation transformed to be run on the client.
     *
     * @param right
     * @return
     */
    def transform(client: Operation): (Operation, Operation) = {

      (server, client) match {
        // Two inserts.
        case (serverOp: Insert, clientOp: Insert) => {

          // Client insertion was to the left of the server's insertion. Thus, the server will directly apply
          // the client's insertion, while the client will apply a transformation of the server's pushed rightwards
          // by the length of the client operation.
          if (clientOp.retain < serverOp.retain) {
            val transformedServerOp = serverOp.copy(retain = serverOp.retain + clientOp.chars.length)
            val transformedClientOp = clientOp

            (transformedClientOp, transformedServerOp)
          }
          // Client insertion was at the same place, or to the right, of the server's insertion. Thus the client will
          // apply the server's operation directly, and the server will apply a transformed client operation that has
          // been shifted rightwards by the length of the server's operation.
          else {
            val transformedServerOp = serverOp
            val transformedClientOp = clientOp.copy(retain = clientOp.retain + serverOp.chars.length)

            (transformedClientOp, transformedServerOp)
          }

        }

        // Server inserts while the client deletes
        // "goat" -> server: goats, client: oat. Result: oats
        // "goat" -> server: lil goat, client: oat. Result: lil oat
        case (serverOp: Insert, clientOp: Delete) => {

          if (clientOp.retain < serverOp.retain) {
            val newServerRetain = if (serverOp.retain - clientOp.chars.length <= 0) { 0 } else { serverOp.retain - clientOp.chars.length }
            val transformedServerOp = serverOp.copy(retain = newServerRetain)
            val transformedClientOp = clientOp

            (transformedClientOp, transformedServerOp)
          }
          else {
            val transformedServerOp = serverOp
            val transformedClientOp = clientOp.copy(retain = clientOp.retain + serverOp.chars.length)

            (transformedClientOp, transformedServerOp)
          }

        }

        // Server deletes while the client inserts. Pretty similar to above.
        case (serverOp: Delete, clientOp: Insert) => {

          if (clientOp.retain < serverOp.retain) {
            val newServerRetain = if (serverOp.retain - clientOp.chars.length <= 0) { 0 } else { serverOp.retain - clientOp.chars.length }
            val transformedServerOp = serverOp.copy(retain = newServerRetain)
            val transformedClientOp = clientOp

            (transformedClientOp, transformedServerOp)
          }
          else {
            val transformedServerOp = serverOp
            val transformedClientOp = clientOp.copy(retain = clientOp.retain - serverOp.chars.length)

            (transformedClientOp, transformedServerOp)
          }

        }

        // Goats
        // Client deletes "oa"
        // Server deletes "at"
        // Result should be: Gs
        case (serverOp: Delete, clientOp: Delete) => {

          // Both deletes begin from the some location. The party with the shorter delete will receive
          // a transformed delete that deletes the extra portion.
          if (clientOp.retain == serverOp.retain) {
            if (serverOp.chars.length > clientOp.chars.length) {
              val transformedServerOp = serverOp.copy(retain = serverOp.retain + clientOp.chars.length)
              val transformedClientOp = Delete(0, "")

              (transformedClientOp, transformedServerOp)
            }
            else if (serverOp.chars.length > clientOp.chars.length) {
              val transformedServerOp = Delete(0, "")
              val transformedClientOp = clientOp.copy(retain = clientOp.retain + serverOp.chars.length)

              (transformedClientOp, transformedServerOp)
            }
            else {
              val transformedServerOp = Delete(0, "")
              val transformedClientOp = Delete(0, "")

              (transformedClientOp, transformedServerOp)
            }
          }

          // The client's delete begins within the server's delete
          else if (clientOp.retain > serverOp.retain &&
            clientOp.retain <= serverOp.retain + serverOp.chars.length) {

            // If the client's delete happened entirely within the server's delete, then the client needs to delete
            // the rest of what the server deleted and the server needs to delete nothing else.
            val transformedServerOp = ComposedOperation(
              Delete(serverOp.retain, serverOp.chars.take(clientOp.retain - serverOp.retain)),
              Delete(clientOp.retain + clientOp.chars.length, serverOp.chars.substring(clientOp.retain - serverOp.retain + clientOp.chars.length))
            )
            val transformedClientOp = Delete(0, "")

            (transformedClientOp, transformedServerOp)
          }

          // The server's delete begins within the client's delete
          else if (serverOp.retain > clientOp.retain &&
            serverOp.retain <= clientOp.retain + clientOp.chars.length) {
            // If the server's delete happened entirely within the client's delete, then the server needs to delete
            // the rest of what the client deleted and the client needs to delete nothing else.
            val transformedServerOp = Delete(0, "")
            val transformedClientOp = ComposedOperation(
              Delete(clientOp.retain, clientOp.chars.take(serverOp.retain - clientOp.retain)),
              Delete(serverOp.retain + serverOp.chars.length, clientOp.chars.substring(serverOp.retain - clientOp.retain + serverOp.chars.length))
            )

            (transformedClientOp, transformedServerOp)
          }

          // The deletes are completely separate
          else {
            // Deletes are adjusted so that they both happen
            if (clientOp.retain < serverOp.retain) {
              val transformedServerOp = serverOp.copy(retain = serverOp.retain - clientOp.chars.length)
              val transformedClientOp = clientOp

              (transformedClientOp, transformedServerOp)
            }
            else {
              val transformedServerOp = serverOp
              val transformedClientOp = clientOp.copy(retain = clientOp.retain - serverOp.chars.length)

              (transformedClientOp, transformedServerOp)
            }
          }

        }
        //
        //        case (serverOp: ComposedOperation, clientOp: ComposedOperation) => {
        //          val transformedServerOp
        //        }
        //
        //        case (serverOp: ComposedOperation, clientOp: Operation) => {
        //
        //        }
        //
        //        case (serverOp: Operation, clientOp: ComposedOperation) => {
        //
        //        }
      }

    }
  }

}

object Operation {
  implicit val reads = new Reads[Operation] {
    def reads(json: JsValue): JsResult[Operation] = {
      val maybeOpType = (json \ "type").asOpt[String]
      val maybeRetain = (json \ "retain").asOpt[Int]
      val maybeChars = (json \ "chars").asOpt[String]

      (maybeOpType, maybeRetain, maybeChars) match {
        case (Some(opType), Some(retain), Some(chars)) => maybeOpType match {
          case Some(opType) if opType == "INSERT" => JsSuccess(Insert(retain, chars))
          case Some(opType) if opType == "DELETE" => JsSuccess(Delete(retain, chars))
          case _ => JsError("Only INSERT and DELETE types are supported.")
        }
        case _ => JsError("Operation must have type, retain, and chars values.")
      }
    }
  }

  implicit val writes = new Writes[Operation] {
    def writes(op: Operation): JsValue = {
      Json.obj(
        "type" -> {op match {
          case op: Insert => "INSERT"
          case op: Delete => "DELETE"
        }},
        "retain" -> op.retain,
        "chars" -> op.chars
      )
    }
  }
}


case class Insert(retain: Int, chars: String) extends Operation {

  def perform(text: String): String = {
    val (left, right) = text.splitAt(retain)
    left + chars + right
  }

}

case class Delete(retain: Int, chars: String) extends Operation {

  def perform(text: String): String = {
    val (left, right) = text.splitAt(retain)
    val (toDelete, rightmost) = right.splitAt(text.length)

    left + rightmost
  }

}

case class ComposedOperation(leftOp: Operation, rightOp: Operation) extends Operation

case class Document(text: String)