# Pruebas con Postman

| Archivo | Uso |
|---|---|
| `FinanzasPersonales.postman_collection.json` | 44 peticiones en 7 carpetas, cada una con sus pruebas (`pm.test`) |
| `local.postman_environment.json` | `baseUrl = http://localhost:8080/api/v1` |
| `produccion.postman_environment.json` | `baseUrl = https://finanzas-personales-lovat-eight.vercel.app/api/v1` (frontend publicado; Vercel reenvía `/api/*` a Render) |

## En Postman (instalado en este equipo)

Antes, arranca la app con `iniciar.cmd` (la API debe responder en http://localhost:8080).

1. Abre Postman → botón *Import* (arriba a la izquierda) → arrastra los tres archivos `.json` de esta carpeta.
2. Elige el entorno (arriba a la derecha).
3. Clic derecho en la colección → *Run collection* → *Run*. Corre en orden: cada carpeta usa las variables que deja la anterior.

Cada corrida crea un usuario nuevo (`prueba+<fecha>@example.com`) y lo elimina al final. El token de renovación viaja en la cookie `HttpOnly` `refresh_token`, que Postman guarda solo.

## Desde la terminal (newman)

```bash
npx newman run postman/FinanzasPersonales.postman_collection.json -e postman/local.postman_environment.json
```

Salida de la última corrida: `docs/evidencias/pruebas/postman-newman.log`.
