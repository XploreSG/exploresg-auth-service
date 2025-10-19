#!/bin/bash
# ============================================
# Memory Optimization Validation Script
# ============================================
# Purpose: Validate memory optimization deployment
# Usage: ./scripts/validate-memory-optimization.sh [namespace] [app-name]

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

NAMESPACE="${1:-exploresg}"
APP_NAME="${2:-exploresg-auth-service}"

echo -e "${CYAN}🔍 Validating Memory Optimization Deployment${NC}"
echo -e "${CYAN}============================================${NC}\n"

# Get pod name
echo -e "${YELLOW}📦 Finding pods...${NC}"
POD_NAME=$(kubectl get pods -n "$NAMESPACE" -l "app=$APP_NAME" -o jsonpath='{.items[0].metadata.name}')

if [ -z "$POD_NAME" ]; then
    echo -e "${RED}❌ No pods found for app: $APP_NAME in namespace: $NAMESPACE${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Using pod: $POD_NAME${NC}\n"

# Check 1: Validate JVM Flags
echo -e "${CYAN}1️⃣ Checking JVM Flags...${NC}"
echo -e "${WHITE}Expected: MaxRAMPercentage=70.0, UseG1GC, ExitOnOutOfMemoryError${NC}"

JVM_FLAGS=$(kubectl exec -n "$NAMESPACE" "$POD_NAME" -- java -XX:+PrintFlagsFinal -version 2>&1 | grep -E "MaxRAMPercentage|UseG1GC|ExitOnOutOfMemoryError" || true)

if [ -n "$JVM_FLAGS" ]; then
    echo -e "${GREEN}✅ JVM Flags Applied:${NC}"
    echo "$JVM_FLAGS" | while read -r line; do
        echo -e "   ${WHITE}$line${NC}"
    done
else
    echo -e "${YELLOW}⚠️  Could not verify JVM flags${NC}"
fi
echo ""

# Check 2: Memory Limits
echo -e "${CYAN}2️⃣ Checking Resource Limits...${NC}"
MEM_LIMIT=$(kubectl get pod -n "$NAMESPACE" "$POD_NAME" -o jsonpath='{.spec.containers[0].resources.limits.memory}')
MEM_REQUEST=$(kubectl get pod -n "$NAMESPACE" "$POD_NAME" -o jsonpath='{.spec.containers[0].resources.requests.memory}')

if [ "$MEM_LIMIT" = "768Mi" ] && [ "$MEM_REQUEST" = "512Mi" ]; then
    echo -e "${GREEN}✅ Memory limits correctly set: Request=$MEM_REQUEST, Limit=$MEM_LIMIT${NC}"
else
    echo -e "${YELLOW}⚠️  Memory limits: Request=$MEM_REQUEST, Limit=$MEM_LIMIT${NC}"
fi
echo ""

# Check 3: Current Memory Usage
echo -e "${CYAN}3️⃣ Checking Current Memory Usage...${NC}"
kubectl top pod -n "$NAMESPACE" "$POD_NAME" || echo -e "${YELLOW}⚠️  Metrics not available${NC}"

MEM_USAGE=$(kubectl top pod -n "$NAMESPACE" "$POD_NAME" --no-headers 2>/dev/null | awk '{print $3}' | sed 's/Mi//' || echo "0")

if [ "$MEM_USAGE" -gt 0 ]; then
    PERCENTAGE=$(echo "scale=1; $MEM_USAGE / 768 * 100" | bc)
    echo -e "${WHITE}Memory Usage: $MEM_USAGE Mi / 768 Mi ($PERCENTAGE%)${NC}"
    
    if (( $(echo "$PERCENTAGE < 70" | bc -l) )); then
        echo -e "${GREEN}✅ Memory usage is healthy (<70%)${NC}"
    elif (( $(echo "$PERCENTAGE < 85" | bc -l) )); then
        echo -e "${YELLOW}⚠️  Memory usage is moderate (70-85%)${NC}"
    else
        echo -e "${RED}❌ Memory usage is high (>85%)${NC}"
    fi
fi
echo ""

# Check 4: Environment Variables
echo -e "${CYAN}4️⃣ Checking Environment Variables...${NC}"
ENV_VARS=$(kubectl exec -n "$NAMESPACE" "$POD_NAME" -- env | grep -E "JAVA_TOOL_OPTIONS|SPRING_PROFILES_ACTIVE|SPRING_JPA_SHOW_SQL|LOGGING_LEVEL" || true)

if [ -n "$ENV_VARS" ]; then
    echo -e "${GREEN}✅ Key Environment Variables:${NC}"
    echo "$ENV_VARS" | while read -r line; do
        echo -e "   ${WHITE}$line${NC}"
    done
else
    echo -e "${YELLOW}⚠️  Could not retrieve environment variables${NC}"
fi
echo ""

# Check 5: Recent Memory/GC Logs
echo -e "${CYAN}5️⃣ Checking for Memory Issues in Logs...${NC}"
MEMORY_LOGS=$(kubectl logs -n "$NAMESPACE" "$POD_NAME" --tail=500 | grep -iE "OutOfMemory|heap|GC pause" | head -5 || true)

if [ -n "$MEMORY_LOGS" ]; then
    echo -e "${YELLOW}⚠️  Found memory-related log entries:${NC}"
    echo "$MEMORY_LOGS" | while read -r line; do
        echo -e "   ${WHITE}$line${NC}"
    done
else
    echo -e "${GREEN}✅ No memory issues detected in recent logs${NC}"
fi
echo ""

# Check 6: Prometheus Metrics
echo -e "${CYAN}6️⃣ Checking Prometheus Metrics...${NC}"
POD_IP=$(kubectl get pod -n "$NAMESPACE" "$POD_NAME" -o jsonpath='{.status.podIP}')

if [ -n "$POD_IP" ]; then
    echo -e "${WHITE}Testing metrics endpoint: http://$POD_IP:8080/actuator/prometheus${NC}"
    
    # Port-forward in background
    kubectl port-forward -n "$NAMESPACE" "$POD_NAME" 8080:8080 >/dev/null 2>&1 &
    PF_PID=$!
    sleep 3
    
    METRICS=$(curl -s http://localhost:8080/actuator/prometheus | grep "jvm_memory_used_bytes" | head -3 || true)
    
    if [ -n "$METRICS" ]; then
        echo -e "${GREEN}✅ Prometheus metrics available${NC}"
        echo -e "${WHITE}Sample JVM memory metrics:${NC}"
        echo "$METRICS" | while read -r line; do
            echo -e "   ${WHITE}$line${NC}"
        done
    else
        echo -e "${YELLOW}⚠️  Could not access metrics endpoint${NC}"
    fi
    
    # Kill port-forward
    kill $PF_PID 2>/dev/null || true
else
    echo -e "${YELLOW}⚠️  Could not determine pod IP${NC}"
fi
echo ""

# Summary
echo -e "${CYAN}============================================${NC}"
echo -e "${CYAN}📊 Validation Summary${NC}"
echo -e "${CYAN}============================================${NC}"
echo -e "${WHITE}Pod: $POD_NAME${NC}"
echo -e "${WHITE}Namespace: $NAMESPACE${NC}"
echo -e "${WHITE}Memory Limit: $MEM_LIMIT${NC}"
echo -e "${WHITE}Memory Request: $MEM_REQUEST${NC}"
if [ "$MEM_USAGE" -gt 0 ]; then
    echo -e "${WHITE}Current Usage: $MEM_USAGE Mi ($PERCENTAGE%)${NC}"
fi

echo -e "\n${GREEN}✅ Validation Complete!${NC}"
echo -e "\n${YELLOW}💡 Next Steps:${NC}"
echo -e "   ${WHITE}1. Monitor memory usage over 24-48 hours${NC}"
echo -e "   ${WHITE}2. Check Grafana dashboards for trends${NC}"
echo -e "   ${WHITE}3. Review logs for any OOM events${NC}"
echo -e "   ${WHITE}4. Compare with baseline metrics${NC}"
