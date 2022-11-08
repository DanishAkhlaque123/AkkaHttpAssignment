import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.Uri.Query.Empty.getAll
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, extractLog, extractRequest, failWith, onComplete, parameter, path, pathEndOrSingleSlash, pathPrefix, post}
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import spray.json._
import sun.security.util.Password

import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.:+
import scala.concurrent.duration._
import java.sql.{Connection, DriverManager, PreparedStatement}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}
case class users(id: Int, name: String, startTime: String)

trait UserJsonProtocol extends DefaultJsonProtocol{
  implicit val UserJson = jsonFormat3(users)
}



object Question1 extends App with UserJsonProtocol with SprayJsonSupport{
  implicit val system = ActorSystem("Assignment")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  val url = "jdbc:mysql://localhost:3306/mynewdb"
  val driver = "com.mysql.cj.jdbc.Driver"
  val username = "root"
  val password = "Alliswell@12345"
  var connection: Connection = _

  try {
    Class.forName(driver)
    connection = DriverManager.getConnection(url, username, password)
  }
  catch {
    case e: Exception => e.printStackTrace
  }
  val statement = connection.createStatement

  def PostUser(user: users, password: Option[String]) = {
    val pass = validation(password)
    if(pass == "Invalid")
      StatusCodes.custom(401,"Invalid password format")
    else
    {
      val add =
        """
             insert into users(id, name, startTime) values (?,?,?)
          """.stripMargin

      val preparedstatement: PreparedStatement = connection.prepareStatement(add)
      preparedstatement.setInt(1, user.id)
      preparedstatement.setString(2, user.name)
      preparedstatement.setString(3, user.startTime)
      preparedstatement.setLong(4, System.currentTimeMillis())
      preparedstatement.execute
      preparedstatement.close()
      StatusCodes.OK
    }

  }

  def update(user: users,UserId: Int, password: Option[String]) = {
    val pass = validation(password)
    val UpdateUser =
      """
      update users set name = ?, startTime = ?, createAt = ? where id = ?
    """.stripMargin

    val preparedstatement: PreparedStatement = connection.prepareStatement(UpdateUser)
    preparedstatement.setInt(1, UserId)
    preparedstatement.setString(2, user.name)
    preparedstatement.setString(3, user.startTime)
    preparedstatement.setLong(4, System.currentTimeMillis())
    preparedstatement.execute
    preparedstatement.close()
    StatusCodes.OK
  }

  def GetAll(username: String) = {
    var result: List[users] = List()

    val preparedstatement = connection.prepareCall("select * from users where exists (select * from users where name = ?)")
    preparedstatement.setString(1,s"$username")
    preparedstatement.execute

    val resultset = preparedstatement.getResultSet
    while(resultset.next()){
      val id = resultset.getInt("id")
      val name = resultset.getString("name")
      val startTime = resultset.getString("startTime")
      result = result :+ users(id, name, startTime)

    }
    result
  }

  def GetUserById(userid: Int) = {
    var result: List[users] = List()
    val resultset = statement.executeQuery(s"select * from users where id = $userid")
    while(resultset.next()){
      val id = resultset.getInt("id")
      val name = resultset.getString("name")
      val startTime = resultset.getString("startTime")
      result = result :+users(id, name, startTime)
    }
    result
  }

  def validation(password :Option[String]): String = password match {
    case Some(password) =>
      if (!password.exists(_.isDigit) && password.length > 8 && password.exists(_.isLetter) && password.exists(_.isLetterOrDigit))
        "Invalid"
      else password
    case None => ""

  }



  implicit val timeout = Timeout(5 seconds)

  val UserServerRoute =
    pathPrefix("api" / "user") {
      get {
        pathEndOrSingleSlash {
          complete(StatusCodes.Unauthorized)
        } ~
          path(Segment) { userName =>
            val users = Future {
              GetAll(userName)
            }
            val result = users.map(user =>
              HttpEntity(
                ContentTypes.`application/json`,
                user.toJson.prettyPrint)
            )
            complete(result)
          } ~
          path(IntNumber) { userid =>
            val users = Future {
              GetUserById(userid)
            }

            val result = users.map(user =>
              HttpEntity(
                ContentTypes.`application/json`,
                user.toJson.prettyPrint
              )
            )
            println(result)
            complete(result)
          }
      } ~
        post {
          parameters('password.?) { password =>
            entity(as[users]) { user =>
              complete(PostUser(user, password))
            }
          }
        } ~
        patch {
          parameters('id.as[Int], 'password.?) { (id, password) =>
            entity(as[users]) { user =>
              complete(update(user, id, password))

            }

          }
        }

    }
  Http().bindAndHandle(UserServerRoute, "localhost", 8081)
}
