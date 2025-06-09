#!/bin/bash

# Production Deployment Script
set -e

# Configuration
ENVIRONMENT=${1:-production}
IMAGE_TAG=${2:-latest}
NAMESPACE="bank-account"

echo "🚀 Starting deployment to $ENVIRONMENT environment..."

# Validate prerequisites
command -v kubectl >/dev/null 2>&1 || { echo "❌ kubectl is required but not installed. Aborting." >&2; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "❌ docker is required but not installed. Aborting." >&2; exit 1; }

# Build and push Docker image
echo "📦 Building Docker image..."
docker build -t ghcr.io/your-username/event-sourcing-bank-account:$IMAGE_TAG .

echo "📤 Pushing Docker image..."
docker push ghcr.io/your-username/event-sourcing-bank-account:$IMAGE_TAG

# Create namespace if it doesn't exist
echo "🏗️  Setting up Kubernetes namespace..."
kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -

# Apply Kubernetes configurations
echo "⚙️  Applying Kubernetes configurations..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/service.yaml

# Update deployment with new image
echo "🔄 Updating deployment..."
kubectl set image deployment/bank-account-app bank-account-app=ghcr.io/your-username/event-sourcing-bank-account:$IMAGE_TAG -n $NAMESPACE

# Apply deployment and ingress
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/ingress.yaml

# Wait for rollout to complete
echo "⏳ Waiting for deployment to complete..."
kubectl rollout status deployment/bank-account-app -n $NAMESPACE --timeout=300s

# Verify deployment
echo "✅ Verifying deployment..."
kubectl get pods -n $NAMESPACE
kubectl get services -n $NAMESPACE

# Run health check
echo "🏥 Running health check..."
HEALTH_CHECK_URL=$(kubectl get ingress bank-account-ingress -n $NAMESPACE -o jsonpath='{.spec.rules[0].host}')
if [ ! -z "$HEALTH_CHECK_URL" ]; then
    curl -f https://$HEALTH_CHECK_URL/actuator/health || echo "⚠️  Health check failed"
else
    echo "⚠️  No ingress URL found, skipping external health check"
fi

echo "🎉 Deployment completed successfully!"
echo "📊 Monitor the application at: https://$HEALTH_CHECK_URL"