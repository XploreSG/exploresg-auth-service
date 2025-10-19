# ============================================
# Memory Optimization Validation Script
# ============================================
# Purpose: Validate memory optimization deployment
# Usage: .\scripts\validate-memory-optimization.ps1 -Namespace exploresg

param(
    [Parameter(Mandatory=$false)]
    [string]$Namespace = "exploresg",
    
    [Parameter(Mandatory=$false)]
    [string]$AppName = "exploresg-auth-service"
)

Write-Host "🔍 Validating Memory Optimization Deployment" -ForegroundColor Cyan
Write-Host "============================================`n" -ForegroundColor Cyan

# Get pod name
Write-Host "📦 Finding pods..." -ForegroundColor Yellow
$pods = kubectl get pods -n $Namespace -l "app=$AppName" -o jsonpath='{.items[*].metadata.name}'

if ([string]::IsNullOrEmpty($pods)) {
    Write-Host "❌ No pods found for app: $AppName in namespace: $Namespace" -ForegroundColor Red
    exit 1
}

$podArray = $pods -split ' '
$podName = $podArray[0]

Write-Host "✅ Using pod: $podName`n" -ForegroundColor Green

# Check 1: Validate JVM Flags
Write-Host "1️⃣ Checking JVM Flags..." -ForegroundColor Cyan
Write-Host "Expected: MaxRAMPercentage=70.0, UseG1GC, ExitOnOutOfMemoryError" -ForegroundColor Gray

$jvmFlags = kubectl exec -n $Namespace $podName -- java -XX:+PrintFlagsFinal -version 2>&1 | Select-String -Pattern "MaxRAMPercentage|UseG1GC|ExitOnOutOfMemoryError"

if ($jvmFlags) {
    Write-Host "✅ JVM Flags Applied:" -ForegroundColor Green
    $jvmFlags | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
} else {
    Write-Host "⚠️  Could not verify JVM flags" -ForegroundColor Yellow
}
Write-Host ""

# Check 2: Memory Limits
Write-Host "2️⃣ Checking Resource Limits..." -ForegroundColor Cyan
$resources = kubectl get pod -n $Namespace $podName -o jsonpath='{.spec.containers[0].resources}'
Write-Host "Resources: $resources" -ForegroundColor White

$memLimit = kubectl get pod -n $Namespace $podName -o jsonpath='{.spec.containers[0].resources.limits.memory}'
$memRequest = kubectl get pod -n $Namespace $podName -o jsonpath='{.spec.containers[0].resources.requests.memory}'

if ($memLimit -eq "768Mi" -and $memRequest -eq "512Mi") {
    Write-Host "✅ Memory limits correctly set: Request=$memRequest, Limit=$memLimit" -ForegroundColor Green
} else {
    Write-Host "⚠️  Memory limits: Request=$memRequest, Limit=$memLimit" -ForegroundColor Yellow
}
Write-Host ""

# Check 3: Current Memory Usage
Write-Host "3️⃣ Checking Current Memory Usage..." -ForegroundColor Cyan
kubectl top pod -n $Namespace $podName

# Calculate percentage
$memUsage = kubectl top pod -n $Namespace $podName --no-headers | ForEach-Object {
    $_ -match '(\d+)Mi' | Out-Null
    [int]$Matches[1]
}

if ($memUsage) {
    $percentage = [math]::Round(($memUsage / 768) * 100, 1)
    Write-Host "Memory Usage: $memUsage Mi / 768 Mi ($percentage%)" -ForegroundColor White
    
    if ($percentage -lt 70) {
        Write-Host "✅ Memory usage is healthy (<70%)" -ForegroundColor Green
    } elseif ($percentage -lt 85) {
        Write-Host "⚠️  Memory usage is moderate (70-85%)" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Memory usage is high (>85%)" -ForegroundColor Red
    }
}
Write-Host ""

# Check 4: Environment Variables
Write-Host "4️⃣ Checking Environment Variables..." -ForegroundColor Cyan
$envVars = kubectl exec -n $Namespace $podName -- env | Select-String -Pattern "JAVA_TOOL_OPTIONS|SPRING_PROFILES_ACTIVE|SPRING_JPA_SHOW_SQL|LOGGING_LEVEL"

if ($envVars) {
    Write-Host "✅ Key Environment Variables:" -ForegroundColor Green
    $envVars | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
} else {
    Write-Host "⚠️  Could not retrieve environment variables" -ForegroundColor Yellow
}
Write-Host ""

# Check 5: Recent Memory/GC Logs
Write-Host "5️⃣ Checking for Memory Issues in Logs..." -ForegroundColor Cyan
$memoryLogs = kubectl logs -n $Namespace $podName --tail=500 | Select-String -Pattern "OutOfMemory|heap|GC pause" -CaseSensitive:$false

if ($memoryLogs) {
    Write-Host "⚠️  Found memory-related log entries:" -ForegroundColor Yellow
    $memoryLogs | Select-Object -First 5 | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
} else {
    Write-Host "✅ No memory issues detected in recent logs" -ForegroundColor Green
}
Write-Host ""

# Check 6: Prometheus Metrics
Write-Host "6️⃣ Checking Prometheus Metrics..." -ForegroundColor Cyan
$podIp = kubectl get pod -n $Namespace $podName -o jsonpath='{.status.podIP}'

if ($podIp) {
    Write-Host "Testing metrics endpoint: http://$podIp:8080/actuator/prometheus" -ForegroundColor Gray
    
    # Port-forward and test metrics
    $job = Start-Job -ScriptBlock {
        param($ns, $pod)
        kubectl port-forward -n $ns $pod 8080:8080
    } -ArgumentList $Namespace, $podName
    
    Start-Sleep -Seconds 3
    
    try {
        $metrics = Invoke-WebRequest -Uri "http://localhost:8080/actuator/prometheus" -TimeoutSec 5 -ErrorAction SilentlyContinue
        
        if ($metrics.StatusCode -eq 200) {
            $jvmMemory = $metrics.Content | Select-String -Pattern "jvm_memory_used_bytes" | Select-Object -First 3
            Write-Host "✅ Prometheus metrics available" -ForegroundColor Green
            Write-Host "Sample JVM memory metrics:" -ForegroundColor White
            $jvmMemory | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
        }
    } catch {
        Write-Host "⚠️  Could not access metrics endpoint: $_" -ForegroundColor Yellow
    } finally {
        Stop-Job $job
        Remove-Job $job
    }
} else {
    Write-Host "⚠️  Could not determine pod IP" -ForegroundColor Yellow
}
Write-Host ""

# Summary
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "📊 Validation Summary" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Pod: $podName" -ForegroundColor White
Write-Host "Namespace: $Namespace" -ForegroundColor White
Write-Host "Memory Limit: $memLimit" -ForegroundColor White
Write-Host "Memory Request: $memRequest" -ForegroundColor White
if ($memUsage) {
    Write-Host "Current Usage: $memUsage Mi ($percentage%)" -ForegroundColor White
}
Write-Host "`n✅ Validation Complete!" -ForegroundColor Green
Write-Host "`n💡 Next Steps:" -ForegroundColor Yellow
Write-Host "   1. Monitor memory usage over 24-48 hours" -ForegroundColor White
Write-Host "   2. Check Grafana dashboards for trends" -ForegroundColor White
Write-Host "   3. Review logs for any OOM events" -ForegroundColor White
Write-Host "   4. Compare with baseline metrics" -ForegroundColor White
