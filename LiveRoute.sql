

DROP USER IF EXISTS 'liveroute_user'@'localhost';
CREATE USER 'liveroute_user'@'localhost' IDENTIFIED BY 'root';
GRANT ALL PRIVILEGES ON liveroute.* TO 'liveroute_user'@'localhost';
FLUSH PRIVILEGES;