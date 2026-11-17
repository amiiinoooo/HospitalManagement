CREATE TABLE ForgottenPasswordToken (
    email VARCHAR(255) NOT NULL,
    token VARCHAR(10) NOT NULL,
    expiry TIMESTAMP NOT NULL,
    PRIMARY KEY (email),
    FOREIGN KEY (email) REFERENCES customer(email)
);