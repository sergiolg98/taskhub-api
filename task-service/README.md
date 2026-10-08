# task-service

Tareas y regla de propiedad. Puerto **8082**, base de datos `task_db`.

```bash
export JWT_SECRET='un-secreto-de-al-menos-32-caracteres-para-hs256'   # el mismo que auth-service
./mvnw spring-boot:run
./mvnw test      # H2, 49 tests (los tokens se firman a mano en TestTokens; auth-service se simula con un servidor HTTP del JDK)
```

Al crear una tarea pregunta a `auth-service` (OpenFeign, `GET /users/{id}`) si el dueño existe, con timeout, reintento, circuit breaker y fallback (Resilience4j); si no puede verificarlo acepta la tarea con `ownerVerified=false`. Necesita `taskhub.auth-service.url`.
Solo **valida** tokens (`JwtParser`): no tiene tabla de usuarios, ni login, ni BCrypt. El dueño de una tarea sale del claim `uid`.
Variables: `JWT_SECRET` (obligatoria), `DB_URL`, `DB_USER`, `DB_PASSWORD`. Ver el [README raíz](../README.md).
