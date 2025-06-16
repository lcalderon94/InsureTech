# Customer Service Observabilidad

Este servicio expone métricas Prometheus y envía trazas a Zipkin mediante OpenTelemetry.

## Despliegue

1. Construir el proyecto:
   ```bash
   ./mvnw clean package
   ```
2. Levantar un contenedor de Zipkin:
   ```bash
   docker run -d -p 9411:9411 openzipkin/zipkin
   ```
3. Ejecutar la aplicación:
   ```bash
   java -jar target/customer-service-0.0.1-SNAPSHOT.jar
   ```
4. Acceder a las métricas en `http://localhost:8081/actuator/prometheus`.
5. Las trazas serán enviadas al endpoint definido en `tracing.zipkin.url` (por defecto `http://localhost:9411/api/v2/spans`).
