# InsureTech Platform

Este proyecto contiene un conjunto de microservicios desarrollados con Spring Boot.
Para facilitar su ejecución local se incluye un archivo `docker-compose.yml` en la raíz del repositorio que levanta todos los servicios junto a Kafka y Redis.

## Requisitos
- Docker y Docker Compose instalados
- Las imágenes de cada microservicio deben estar previamente construidas con el nombre indicado en el compose (por ejemplo `api-gateway:latest`).

## Uso rápido
1. Posicionarse en la raíz del repositorio.
2. Ejecutar:
   ```bash
   docker-compose up
   ```
   Esto iniciará el `config-server`, los demás microservicios, Kafka, Redis y utilidades como Kafdrop.
3. Para detener todo:
   ```bash
   docker-compose down
   ```

Todos los servicios están configurados para obtener su configuración del `config-server` mediante la variable de entorno `SPRING_CLOUD_CONFIG_URI`.
El servidor de configuración monta el directorio `config-repo` incluido en este repositorio.

