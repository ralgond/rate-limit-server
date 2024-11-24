use rate_limit;

DROP TABLE IF EXISTS rules;

CREATE TABLE rules (
	`id` INT NOT NULL AUTO_INCREMENT,
	`key_type` enum ("IP", "SI"),
	`method` CHAR(10) NOT NULL,
	`path_pattern` VARCHAR(1024) NOT NULL,
	`deleted` BOOLEAN NOT NULL,
	`create_time` DATE NOT NULL,
	`update_time` DATE NOT NULL,
	`burst` INT NOT NULL,
	`token_count` INT NOT NULL,
	`token_time_unit` INT NOT NULL,
	PRIMARY KEY(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


INSERT INTO rules (`key_type`, `method`, path_pattern, deleted, create_time, update_time, burst, token_count, token_time_unit) VALUES 
("IP", "GET", "/.*", false, NOW(), NOW(), 15, 30, 60);

INSERT INTO rules (`key_type`, `method`, path_pattern, deleted, create_time, update_time, burst, token_count, token_time_unit) VALUES 
("SI", "POST", "/article/new", false, NOW(), NOW(), 3, 3, 60);

INSERT INTO rules (`key_type`, `method`, path_pattern, deleted, create_time, update_time, burst, token_count, token_time_unit) VALUES 
("SI", "GET", "/article/\\d+", false, NOW(), NOW(), 5, 5, 60);

