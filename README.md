# homework-backend

Spring Boot 3 + MyBatis-Plus + MySQL + Lombok project template generated from the `home_work` database schema.

## Run

```bash
mvn spring-boot:run
```

The default datasource points to `jdbc:mysql://127.0.0.1:3306/home_work` with username `root` and password `123456`.

You can override it with environment variables:

```bash
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=home_work
export DB_USERNAME=root
export DB_PASSWORD=123456
```

## Generated content

- Entity classes: `src/main/java/com/homework/entity`
- Mapper interfaces: `src/main/java/com/homework/mapper`
- Enum classes: `src/main/java/com/homework/common/enums`
- Shared base entity: `src/main/java/com/homework/common/entity/BaseEntity.java`
