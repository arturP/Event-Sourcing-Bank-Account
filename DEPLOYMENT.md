# Production Deployment Guide

This guide covers the production deployment setup for the Event Sourcing Bank Account application.

## Prerequisites

- Docker and Docker Compose
- Kubernetes cluster (for K8s deployment)
- kubectl configured
- PostgreSQL database
- Redis cache
- SSL certificates (for HTTPS)

## Quick Start with Docker Compose

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Event-Sourcing-Bank-Account
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your production values
   ```

3. **Start the application**
   ```bash
   docker-compose up -d
   ```

4. **Verify deployment**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

## Kubernetes Deployment

### 1. Build and Push Docker Image

```bash
# Build the image
docker build -t ghcr.io/your-username/event-sourcing-bank-account:latest .

# Push to registry
docker push ghcr.io/your-username/event-sourcing-bank-account:latest
```

### 2. Configure Kubernetes Secrets

```bash
# Create secrets for sensitive data
kubectl create secret generic bank-account-secrets \
  --from-literal=DB_PASSWORD=your-db-password \
  --from-literal=ADMIN_PASSWORD=your-admin-password \
  -n bank-account
```

### 3. Deploy to Kubernetes

```bash
# Deploy using the provided script
./scripts/deploy.sh production latest

# Or deploy manually
kubectl apply -f k8s/
```

### 4. Configure Ingress and SSL

Update `k8s/ingress.yaml` with your domain and SSL certificate:

```yaml
spec:
  tls:
  - hosts:
    - yourdomain.com
    secretName: bank-account-tls
```

## Environment Configurations

### Development
- Profile: `default`
- Database: H2 in-memory
- Cache: Local cache
- Security: Basic authentication

### Production
- Profile: `prod`
- Database: PostgreSQL
- Cache: Redis
- Security: JWT + HTTPS
- Monitoring: Prometheus + Grafana

### Docker
- Profile: `docker`
- Database: PostgreSQL container
- Cache: Redis container
- Networking: Docker bridge network

## Monitoring and Health Checks

### Health Endpoints
- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Overall Health**: `/actuator/health`
- **Metrics**: `/actuator/prometheus`

### Monitoring Stack
- **Prometheus**: Metrics collection (port 9090)
- **Grafana**: Visualization dashboard (port 3000)
- **Application**: Main service (port 8080)

Access Grafana at `http://localhost:3000` (admin/admin)

## Security Configuration

### Database Security
- Use strong passwords
- Enable SSL/TLS for database connections
- Restrict database access to application network

### Application Security
- JWT token authentication
- HTTPS enforcement
- Rate limiting
- Input validation
- Security headers

### Container Security
- Non-root user execution
- Minimal base image (JRE slim)
- Security scanning in CI/CD
- Regular image updates

## Performance Tuning

### JVM Settings
```bash
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
```

### Database Connection Pool
- Maximum pool size: 20
- Minimum idle: 5
- Connection timeout: 20s
- Max lifetime: 20 minutes

### Redis Configuration
- Connection pool: 8 max active
- Timeout: 2000ms
- Lettuce connection pool

## Backup and Recovery

### Database Backup
```bash
# PostgreSQL backup
pg_dump -h localhost -U bankuser bankaccount > backup.sql

# Restore
psql -h localhost -U bankuser bankaccount < backup.sql
```

### Redis Backup
```bash
# Redis backup (automatic with AOF)
redis-cli BGSAVE
```

## Troubleshooting

### Common Issues

1. **Application won't start**
   - Check database connectivity
   - Verify environment variables
   - Review application logs

2. **Health check failures**
   - Verify database connection
   - Check Redis connectivity
   - Review resource usage

3. **Performance issues**
   - Monitor JVM metrics
   - Check database connection pool
   - Review query performance

### Log Locations
- Application logs: `/app/logs/bank-account-app.log`
- Container logs: `docker logs <container-id>`
- Kubernetes logs: `kubectl logs <pod-name> -n bank-account`

## CI/CD Pipeline

The application includes a GitHub Actions pipeline that:

1. **Test Phase**
   - Runs unit and integration tests
   - Generates test reports
   - Uploads coverage data

2. **Security Phase**
   - OWASP dependency check
   - Security vulnerability scanning
   - SARIF report upload

3. **Build Phase**
   - Builds Docker image
   - Pushes to container registry
   - Generates deployment artifacts

4. **Deploy Phase**
   - Deploys to production environment
   - Runs health checks
   - Notifies on completion

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `DB_URL` | Database connection URL | - |
| `DB_USERNAME` | Database username | `bankuser` |
| `DB_PASSWORD` | Database password | - |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `ADMIN_USERNAME` | Admin username | `admin` |
| `ADMIN_PASSWORD` | Admin password | - |

## Support and Maintenance

### Regular Maintenance Tasks
- Update dependencies monthly
- Review security advisories
- Monitor application metrics
- Backup verification
- Certificate renewal (SSL)

### Contact Information
- Team: Backend Development Team
- Email: backend-team@company.com
- Slack: #backend-support