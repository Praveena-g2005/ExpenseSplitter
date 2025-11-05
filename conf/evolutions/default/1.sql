# --- !Ups

CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER', -- use enum to avoid wrong inputs
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_role (role)
);

CREATE TABLE IF NOT EXISTS expenses (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  expense_name VARCHAR(255), 
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
  sender BIGINT NOT NULL,
  receiver BIGINT NOT NULL, 
  expense_id BIGINT NOT NULL,
  amount DOUBLE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sender) REFERENCES users(id),
  FOREIGN KEY (receiver) REFERENCES users(id),
  FOREIGN KEY (expense_id) REFERENCES expenses(id)
);

CREATE TABLE IF NOT EXISTS notifications (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  expense_id BIGINT NOT NULL,
  receiver BIGINT NOT NULL, 
  message TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (expense_id) REFERENCES expenses(id),
  FOREIGN KEY (receiver) REFERENCES users(id)
);

-- Create default admin user
-- Email: admin@example.com
-- Password: Admin@123
-- make this like admin can set role for another user as admin
INSERT INTO users (name, email, password_hash, role) 
VALUES (
  'System Admin', 
  'admin@example.com', 
  '$2a$12$jSm7s/0/rFh7QS0tnecZhudOAC/pNBG8a1N1aBsOxWShsDEBd/Ie.',
  'ADMIN'
);

# --- !Downs

DROP TABLE IF EXISTS balances;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS expense_participants;
DROP TABLE IF EXISTS expenses;
DROP TABLE IF EXISTS users;