# auth-service

Registro, login y emisión del JWT. Puerto **8081**, base de datos `auth_db`.

```bash
export JWT_SECRET='un-secreto-de-al-menos-32-caracteres-para-hs256'   # el mismo que task-service
./mvnw spring-boot:run
./mvnw test      # H2, 36 tests
```

Única responsabilidad de firma del sistema: `JwtTokenIssuer`. Variables: `JWT_SECRET` (obligatoria), `DB_URL`, `DB_USER`, `DB_PASSWORD`.
Migraciones en `src/main/resources/db/migration` (`users` y usuarios de ejemplo). Ver el [README raíz](../README.md).

Imagen: `docker build -t taskhub/auth-service .` (puerto 8081, usuario no root, `HEALTHCHECK` en `/actuator/health`). Ver el [README raíz](../README.md#docker-clase-12).
