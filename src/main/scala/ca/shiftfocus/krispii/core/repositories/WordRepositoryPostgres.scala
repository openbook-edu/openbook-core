package ca.shiftfocus.krispii.core.repositories

import ca.shiftfocus.krispii.core.error.RepositoryError
import ca.shiftfocus.krispii.core.models.LinkWord
import com.github.mauricio.async.db.{ RowData, Connection }

import scala.concurrent.Future
import scalaz.\/

class WordRepositoryPostgres extends WordRepository with PostgresRepository[LinkWord] {
  override val entityName = "LinkWord"
  val Fields = "word, lang"
  val QMarks = "?, ?"
  val Table = "words"

  val SelectOneByLang =
    s"""
       |SELECT $Fields
       |FROM $Table
       |WHERE lang = ?
       |OFFSET floor(random()*(select COUNT(*) from $Table WHERE lang = ? and word not in (select link from links where link is not null)))
       |LIMIT 1
     """.stripMargin

  val Insert =
    s"""
       |INSERT INTO $Table ($Fields)
       |VALUES ($QMarks)
       |RETURNING $Fields
    """.stripMargin

  val Delete =
    s"""
       |DELETE FROM words
       |WHERE word = ?
       |AND lang = ?
       |RETURNING $Fields
    """.stripMargin

  override def constructor(row: RowData): LinkWord = {
    LinkWord(
      row("word").asInstanceOf[String],
      row("lang").asInstanceOf[String]
    )
  }

  /**
   * Get a random string for a given language
   * @param lang language, obv
   * @param conn
   * @return
   */
  override def get(lang: String)(implicit conn: Connection): Future[\/[RepositoryError.Fail, LinkWord]] = {
    queryOne(SelectOneByLang, Seq[Any](lang, lang))
  }

  /**
   * Insert a word. It is for a future if we ever need it
   * @param word
   * @param conn
   * @return
   */
  override def insert(word: LinkWord)(implicit conn: Connection): Future[\/[RepositoryError.Fail, LinkWord]] = {
    val params = Seq[Any](word.word, word.lang)
    queryOne(Insert, params)
  }

  /**
   * Delete a word, maybe later we can use it
   * @param word
   * @param conn
   * @return
   */
  override def delete(word: LinkWord)(implicit conn: Connection): Future[\/[RepositoryError.Fail, LinkWord]] = {
    val params = Seq[Any](word.word, word.lang)
    queryOne(Delete, params)
  }
}
