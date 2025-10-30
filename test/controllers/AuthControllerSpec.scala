package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.Json
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.apache.pekko.actor.ActorSystem
import play.api.inject.bind

class AuthControllerSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting {

  // Create ActorSystem and Materializer explicitly for WSClient
  implicit lazy val actorSystem: ActorSystem = ActorSystem("TestSystem")
  implicit lazy val mat: Materializer = SystemMaterializer(
    actorSystem
  ).materializer

  // Override fakeApplication to provide Materializer to Guice
  override def fakeApplication() =
    new GuiceApplicationBuilder()
      .overrides(bind[Materializer].toInstance(mat))
      .build()

  "AuthController POST /auth/register" should {

    "register a new user successfully" in {
      val request = FakeRequest(POST, "/auth/register")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "name" -> "Test User",
            "email" -> s"test${System.currentTimeMillis()}@test.com",
            "password" -> "Password123"
          )
        )

      val result = route(app, request).get
      status(result) mustBe CREATED
      val json = contentAsJson(result)
      (json \ "message").as[String] must include("registered successfully")
    }

    "reject registration with weak password" in {
      val request = FakeRequest(POST, "/auth/register")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "name" -> "Test User",
            "email" -> "test@test.com",
            "password" -> "weak"
          )
        )

      val result = route(app, request).get
      status(result) mustBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "error").as[String] must include("8 characters")
    }

    "reject registration with invalid email" in {
      val request = FakeRequest(POST, "/auth/register")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "name" -> "Test User",
            "email" -> "invalid-email",
            "password" -> "Password123"
          )
        )

      val result = route(app, request).get
      status(result) mustBe BAD_REQUEST
    }

    "reject duplicate email registration" in {
      val email = s"duplicate${System.currentTimeMillis()}@test.com"

      // First registration
      val request1 = FakeRequest(POST, "/auth/register")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "name" -> "User One",
            "email" -> email,
            "password" -> "Password123"
          )
        )
      await(route(app, request1).get)

      // Second registration with same email
      val request2 = FakeRequest(POST, "/auth/register")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "name" -> "User Two",
            "email" -> email,
            "password" -> "Password123"
          )
        )

      val result = route(app, request2).get
      status(result) mustBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "error").as[String] must include("already registered")
    }
  }

  "AuthController POST /auth/login" should {

    "login with valid credentials" in {
      val email = s"login${System.currentTimeMillis()}@test.com"

      // Register user first
      val registerRequest = FakeRequest(POST, "/auth/register")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "name" -> "Test User",
            "email" -> email,
            "password" -> "Password123"
          )
        )
      await(route(app, registerRequest).get)

      // Login
      val loginRequest = FakeRequest(POST, "/auth/login")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "email" -> email,
            "password" -> "Password123"
          )
        )

      val result = route(app, loginRequest).get
      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "accessToken").asOpt[String] mustBe defined
      (json \ "refreshToken").asOpt[String] mustBe defined
      (json \ "user" \ "email").as[String] mustBe email
    }

    "reject login with wrong password" in {
      val email = s"wrongpass${System.currentTimeMillis()}@test.com"

      val registerRequest = FakeRequest(POST, "/auth/register")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "name" -> "Test User",
            "email" -> email,
            "password" -> "Password123"
          )
        )
      await(route(app, registerRequest).get)

      val loginRequest = FakeRequest(POST, "/auth/login")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "email" -> email,
            "password" -> "WrongPassword"
          )
        )

      val result = route(app, loginRequest).get
      status(result) mustBe UNAUTHORIZED
    }

    "reject login for non-existent user" in {
      val loginRequest = FakeRequest(POST, "/auth/login")
        .withHeaders("Content-Type" -> "application/json")
        .withJsonBody(
          Json.obj(
            "email" -> "nonexistent@test.com",
            "password" -> "Password123"
          )
        )

      val result = route(app, loginRequest).get
      status(result) mustBe UNAUTHORIZED
    }
  }
}
