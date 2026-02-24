CREATE TABLE tutor_appointment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT NOT NULL,
  tutor_id BIGINT NOT NULL,
  parent_job_posting_id BIGINT,
  tutor_job_posting_id BIGINT,
  title VARCHAR(100),
  subject_id BIGINT NOT NULL,
  class_mode VARCHAR(50),
  city VARCHAR(100),
  address VARCHAR(255),
  start_time TIMESTAMP NOT NULL,
  duration_minutes INT NOT NULL DEFAULT 60,
  status TINYINT NOT NULL DEFAULT 1,
  created_by BIGINT NOT NULL,
  room_id BIGINT,
  proposed_start_time TIMESTAMP,
  proposed_by BIGINT,
  cancel_by BIGINT,
  remark VARCHAR(255),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE position_post (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT,
  name VARCHAR(255) NOT NULL,
  grade VARCHAR(50),
  description VARCHAR(500),
  sort INT,
  enable_status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teacher_job_posting (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tutor_id BIGINT NOT NULL,
  subject_id BIGINT,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(2000),
  price_per_hour DECIMAL(10,2),
  mode VARCHAR(20),
  city VARCHAR(100),
  available_time VARCHAR(2000),
  max_students INT,
  status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE student_job_posting (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT NOT NULL,
  subject_id BIGINT,
  subject_name VARCHAR(100) NOT NULL,
  subject_is_other TINYINT NOT NULL DEFAULT 0,
  title VARCHAR(255) NOT NULL,
  description VARCHAR(2000),
  student_gender VARCHAR(8) NOT NULL,
  grade_code VARCHAR(16),
  available_time VARCHAR(2000),
  teacher_gender_preference VARCHAR(8) DEFAULT 'both',
  teacher_requirement_detail VARCHAR(2000),
  child_age INT,
  class_mode VARCHAR(20),
  city VARCHAR(100),
  address VARCHAR(255),
  frequency_per_week INT NOT NULL DEFAULT 2,
  budget_min DECIMAL(10,2),
  budget_max DECIMAL(10,2),
  stage_code VARCHAR(32),
  education_requirement VARCHAR(32),
  publisher_identity VARCHAR(16) NOT NULL DEFAULT 'PARENT',
  schedule VARCHAR(2000),
  status TINYINT NOT NULL DEFAULT 1,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tutor_favorite_demand (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tutor_id BIGINT NOT NULL,
  demand_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE parent_favorite_tutor (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id BIGINT NOT NULL,
  tutor_id BIGINT NOT NULL,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255),
  phone VARCHAR(32),
  avatar VARCHAR(1000),
  sex INT,
  open_id VARCHAR(255),
  active_status INT,
  last_opt_time TIMESTAMP,
  ip_info VARCHAR(2000),
  item_id BIGINT,
  status INT,
  user_type INT,
  ref_id BIGINT,
  password VARCHAR(255),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE teacher_profile (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  real_name VARCHAR(100),
  education VARCHAR(100),
  subject VARCHAR(255),
  experience_years INT,
  rate_per_hour DECIMAL(10,2),
  introduction VARCHAR(2000),
  default_greeting VARCHAR(1024),
  certificate_urls VARCHAR(2000),
  basic_completed TINYINT DEFAULT 0,
  realname_verify_status TINYINT DEFAULT 0,
  realname_verify_method VARCHAR(20),
  realname_verify_id_front_url VARCHAR(255),
  realname_verify_id_back_url VARCHAR(255),
  realname_verify_idno_cipher VARCHAR(512),
  realname_verify_idno_masked VARCHAR(32),
  realname_verify_reject_reason VARCHAR(255),
  realname_verify_submit_time TIMESTAMP NULL,
  realname_verify_time TIMESTAMP NULL,
  edu_verify_status TINYINT DEFAULT 0,
  edu_verify_proof_urls VARCHAR(2000),
  edu_verify_reject_reason VARCHAR(255),
  edu_verify_submit_time TIMESTAMP NULL,
  edu_verify_time TIMESTAMP NULL,
  status INT,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
