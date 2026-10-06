# api-gateway

Punto único de entrada (Spring Cloud Gateway, WebFlux). Puerto **8080**. Rutas y CORS en `../config-repo/api-gateway*.properties`; necesita el config-server arrancado.

```bash
./mvnw spring-boot:run
./mvnw test      # 8 tests con dos servicios falsos (servidores HTTP del JDK); no necesita el config-server
```

Filtros globales: `RequestIdFilter` (`X-Request-Id`), `BearerRequiredFilter` (401 si falta el header fuera de `/auth/**`) y `GatewayErrorHandler` (formato de error único).
Ver el [README raíz](../README.md).

Imagen: `docker build -t taskhub/api-gateway .` (puerto 8080, usuario no root, `HEALTHCHECK` en `/actuator/health`). Ver el [README raíz](../README.md#docker-clase-12).
