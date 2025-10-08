package app.utils

import com.github.t3hnar.bcrypt._

object PasswordHasher {
  private val rounds = 12

  def hash(password: String): String = {
    password.bcrypt(rounds)
  }

  def verify(password: String, hash: String): Boolean = {
    password.isBcrypted(hash)
  }
}
