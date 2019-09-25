# --- !Ups
CREATE TABLE IF NOT EXISTS users (
    id                  BIGINT UNSIGNED     NOT NULL    AUTO_INCREMENT
    , discord_user_id   BIGINT UNSIGNED     NOT NULL
    , created           DATETIME            NOT NULL    DEFAULT CURRENT_TIMESTAMP

    , PRIMARY KEY (id)
    , UNIQUE(discord_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# --- !Downs
DROP TABLE IF EXISTS users;
