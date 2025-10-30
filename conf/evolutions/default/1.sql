# --- !Ups

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER', --- use enum to avoid wrong inputs
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_role (role)
);

CREATE TABLE IF NOT EXISTS expenses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  description VARCHAR(255), --- need to change this description to name
  amount DOUBLE NOT NULL,
  paid_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (paid_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS expense_participants (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  expense_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  shared_amt DOUBLE NOT NULL,
  FOREIGN KEY (expense_id) REFERENCES expenses(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS balances (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  from_user BIGINT NOT NULL, --- change to sender 
  to_user BIGINT NOT NULL, --- change to receiver
  expense_id BIGINT NOT NULL,
  amount DOUBLE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (from_user) REFERENCES users(id),
  FOREIGN KEY (to_user) REFERENCES users(id),
  FOREIGN KEY (expense_id) REFERENCES expenses(id)
);

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  expense_id BIGINT NOT NULL,
  recipient BIGINT NOT NULL, --- change to notifier
  message TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (expense_id) REFERENCES expenses(id),
  FOREIGN KEY (recipient) REFERENCES users(id)
);
--- need to remove refresh tokens and revoked tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  revoked BOOLEAN DEFAULT FALSE,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_token (token),
  INDEX idx_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS revoked_tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token VARCHAR(500) NOT NULL,
  user_id BIGINT NOT NULL,
  token_type VARCHAR(20) NOT NULL DEFAULT 'REFRESH',
  revoked_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_revoked_token (token),
  INDEX idx_user_id (user_id),
  INDEX idx_expires_at (expires_at)
);

-- Create default admin user
-- Email: admin@example.com
-- Password: Admin@123
INSERT INTO users (name, email, password_hash, role) 
VALUES (
  'System Admin', 
  'admin@example.com', 
  '$2a$12$jSm7s/0/rFh7QS0tnecZhudOAC/pNBG8a1N1aBsOxWShsDEBd/Ie.',
  'ADMIN'
);

# --- !Downs

DROP TABLE IF EXISTS revoked_tokens;
DROP TABLE IF EXISTS balances;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS expense_participants;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS users;