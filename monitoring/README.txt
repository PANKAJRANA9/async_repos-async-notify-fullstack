MONITORING FOLDER - WHAT CODE IS REQUIRED?

YES - 4 files required. Without them Grafana is empty.

monitoring/
  prometheus.yml                                  <- REQUIRED - tells Prometheus to scrape backend:8080/actuator/prometheus
  grafana/provisioning/datasources/datasource.yml <- REQUIRED - auto-connects Grafana -> Prometheus (no manual setup)
  grafana/provisioning/dashboards/dashboard.yml   <- REQUIRED - tells Grafana where to load dashboard JSON
  grafana/dashboards/async-notify.json            <- REQUIRED - 8 panels: Sent, Dedup, Retry, DLQ, Outbox, JVM, Rate, HTTP

HOW IT WORKS:
  Backend (Spring Boot) -> /actuator/prometheus exposes:
    async_notify_sent_total
    async_notify_dedup_total
    async_notify_retry_total
    async_notify_dlq_total
    async_notify_outbox_pending
    jvm_memory_used_bytes
    http_server_requests_seconds_count
  Prometheus (9090) scrapes backend every 15s
  Grafana (3001) reads from Prometheus and shows dashboard

DOCKER-COMPOSE MUST MOUNT THESE:
  prometheus:
    volumes: ["./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml"]
  grafana:
    volumes:
      - "./monitoring/grafana/provisioning:/etc/grafana/provisioning"
      - "./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards"
    environment: {GF_SECURITY_ADMIN_USER: admin, GF_SECURITY_ADMIN_PASSWORD: admin123}

BACKEND MUST HAVE (already in backend-fullcode-fixed.zip):
  pom.xml: micrometer-registry-prometheus + spring-boot-starter-actuator
  application.yml: management.endpoints.web.exposure.include: health,info,metrics,prometheus
  MetricsService.java: Counters for sent/dedup/retry/dlq

VERIFY:
  curl http://localhost:8080/actuator/prometheus | findstr async_notify
  curl http://localhost:9090/api/v1/query?query=async_notify_sent_total
  Open http://localhost:3001 -> admin/admin123 -> Dashboards -> AsyncNotify
