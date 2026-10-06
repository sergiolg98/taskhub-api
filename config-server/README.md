# config-server

Spring Cloud Config Server con backend `native` (archivos). Puerto **8888**. Sirve `../config-repo/` (o `CONFIG_REPO_PATH`).

```bash
./mvnw spring-boot:run
curl localhost:8888/task-service/dev
./mvnw test      # 3 tests: perfiles, orden de los archivos y que config-repo no tenga secretos
```

La variante de producción sería el backend Git (`spring.cloud.config.server.git.uri`); aquí se usa archivos para funcionar sin red. Ver el [README raíz](../README.md).
