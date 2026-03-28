CREATE TABLE user_details (
                              user_id BIGSERIAL PRIMARY KEY,
                              name VARCHAR(255) NOT NULL,
                              email VARCHAR(255) UNIQUE NOT NULL,
                              password VARCHAR(255) NOT NULL,
                              mobile_number VARCHAR(20),
                              registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              last_modified TIMESTAMP
);

CREATE TABLE category (
                          category_id BIGSERIAL PRIMARY KEY,
                          category_name VARCHAR(255) UNIQUE NOT NULL
);

CREATE TABLE expense (
                         expense_id BIGSERIAL PRIMARY KEY,
                         description VARCHAR(255) NOT NULL,
                         amount DOUBLE PRECISION NOT NULL,
                         date DATE NOT NULL,
                         category_id BIGINT,
                         user_id BIGINT,
                         CONSTRAINT fk_expense_category FOREIGN KEY (category_id) REFERENCES category(category_id),
                         CONSTRAINT fk_expense_user FOREIGN KEY (user_id) REFERENCES user_details(user_id)
);

CREATE TABLE aggregate_expense (
                                   id BIGSERIAL PRIMARY KEY,
                                   user_id BIGINT,
                                   expense_month SMALLINT NOT NULL,
                                   expense_year INTEGER NOT NULL,
                                   amount DOUBLE PRECISION NOT NULL,
                                   CONSTRAINT fk_aggregate_user FOREIGN KEY (user_id) REFERENCES user_details(user_id)
);