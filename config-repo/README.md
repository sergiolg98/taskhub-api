# config-repo

Configuración de los servicios, servida por `config-server`. Nombre del archivo = `{spring.application.name}[-{perfil}].properties`.

Aquí **no** van secretos (`JWT_SECRET`, `DB_USER`, `DB_PASSWORD`): cada servicio los recibe por variable de entorno.
