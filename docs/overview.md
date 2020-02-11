# krispii-core

The core is a plain Scala sbt module that handles data services and domain logic for the krispii platform.  This is not intended to be run as a standalone project: written so that the underlying data storage and domain logic is independent from the interface it is served by. In practice this means that it should be imported by the krispii-api project, which will expose the functionality provided by core to the outside world via json-speaking HTTP endpoints.

Core has two layers: repository layer, and services layer. Each layer has definition traits, definining the interfaces that each service and repository should implement, along with implementation classes. `AuthService` and `AuthServiceDefault`. `UserRepository` and `UserRepositoryPostgres`. They describe dependencies to abstract types, and concrete implementations are injected at "the top of the world" where, for example, a configuration value could decide between the use of MySQL or Postgres repositories..


## Repository layer

The repository layer maps entities into the database and back. The repository layer should contain no domain logic and should only be concerned with "given a User object, how do I persist it? how do I retrieve it?" Currently repositories are implemented for the postgresql 9.4+ database. For certain new features we are taking advantage of postgres' jsonb storage.

Method signatures look like this:

    def list(implicit conn: Connection): Future[\/[RepositoryError.Fail, A]]
      
Future goal: clean up and simplify method signatures. Database connection and optional cache should be passed in via the reader monad, and for easier composition the methods should return a monad transformer like (with scalaz) `EitherT[Future, RepoFail, A]`, (with cats) `XorT[Future, RepoFail, A]`, or switch to scalaz's Task like `Task[A]`. Have a look at lceeq-accounts type aliases:

    Expect[A] = EitherT[Future, Fail, A]
    ExpectReader[A] = Kleisli[Expect, Connection, A]
    
Note that `scalaz.\/` and `scalaz.EitherT` are analagous to `cats.data.Xor` and `cats.data.XorT`.
    
So our repository return types can be simplified to look like: `ExpectReader[User]` which represents an underlying function with type `Connection => EitherT[Future, Fail, A]` (or `Connection => Task[A]` if you choose to go the Task route).

Why do we do all of this? So that we can have nice, clean code:

    val op = for {
      user <- userRepo.find(email)
      courses <- courseRepo.list(user.id)
      projects <- serializedT(courses)(projectRepo.list)
    } yield projects
    
    val result = op.run(connection)
    
What's the type of `op` and `result`? Essentially op is a function that takes a connection, and will eventually return a list of projects. Or, if any step failed, it will return the first error encountered and abort the rest of the chain. If you're familiar with Haskell, the for-comprehension is analagous to its do-notation.


## Service layer

The service layer contains all domain logic. You'll notice fewer classes in the service layer than the repo layer: that's because service layer is organized more according to use cases and related logic. The `AuthService` contains code dealing with users, roles, authentication. `WorkService` handles user-created responses and notes. Etc. 

The services have method signatures that look like:

    def authenticate(identifier: String, password: String): Future[\/[ErrorUnion#Fail, User]]
    
They make calls to repositories and to other services. There's nothing special to note here besides the fact that the method signatures can be cleaned up a bit with a type alias to a monad transformer like `type Expect[A] = EitherT[Future, Fail, A]`.

You'll notice that right now there are `lift` calls everywhere. `lift` basically lifts values of type `Future[A \/ B]` into the monad transformer `EitherT[Future, A, B]` so that they can be composed in for-comprehensions. In order to get plain results on the left-hand side, each line in the for-comprehension needs to be in the same monad. Monad transformers lets us stack up multiple types into a single mappable and flatMappable type.

    Future[A \/ B] --> EitherT[Future, A, B]
    Future[Option[B]] --> OptionT[Future, B]

## Models

Core also stores models which are the domain representation of various entity types. They should act like structs holding only data, no logic, with the exception of stuff like json readers and writers.


## Documents

Documents are edited and stored using [Operational Transformation](https://operational-transformation.github.io/visualization.html). This means that,
in the database a document is represented by the chain of edits from its creation to "now", along with a current copy of the full document. Storing
this chain is what allows live and collaborative editing. The Document and Revision repositories both take advantage of my 
open source ot library which is currently published on our maven server. It implements a rich-text version of OT that uses annotations on operations to add
contextual information like text formatting. Right now editing happens over websockets but it's possible to implement a plain HTTP api as well.


## Publishing

Core should be published as a library that you can import into SBT projects like:

  libraryDependencies += "ca.shiftfocus" %% "krispii-core" % "2.0.1"
