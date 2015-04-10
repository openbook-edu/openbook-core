import ca.shiftfocus.krispii.core.repositories.DocumentRepositoryPostgres

class DocumentRepositorySpec
  extends TestEnvironment
{
  val documentRepository = new DocumentRepositoryPostgres
}
