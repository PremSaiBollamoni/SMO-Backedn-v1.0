$baseUrl = "http://localhost:8080"

function Log($message, $color = "White") {
    Write-Host "[TEST] $message" -ForegroundColor $color
}

# 1. Test Health Endpoint
Log "Testing health endpoint at $baseUrl/api/health..."
try {
    $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -Method Get -TimeoutSec 5
    Log "Health endpoint responded: $health" "Green"
} catch {
    Log "Failed to reach health endpoint. Is the backend server running? Error: $_" "Red"
    exit 1
}

# 2. Test Login Endpoint
Log "Logging in with Employee ID 1006 (HR)..."
$loginBody = @{
    loginid = "1006"
    password = "pass"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -TimeoutSec 5
    $token = $loginResponse.token
    if ($token) {
        Log "Login successful! Token retrieved." "Green"
    } else {
        Log "Login succeeded but token was empty." "Yellow"
        exit 1
    }
} catch {
    Log "Login request failed. Error: $_" "Red"
    exit 1
}

# Helper to execute authenticated requests
function Call-Endpoint($endpoint, $method = "Get", $body = $null) {
    Log "--------------------------------------------"
    Log "Calling $method $endpoint..."
    $headers = @{
        Authorization = "Bearer $token"
    }
    $params = @{
        Uri = "$baseUrl$endpoint"
        Method = $method
        Headers = $headers
        TimeoutSec = 5
    }
    if ($body) {
        $params["Body"] = $body
        $params["ContentType"] = "application/json"
    }
    try {
        $res = Invoke-RestMethod @params
        Log "Success! Response:" "Green"
        $res | ConvertTo-Json -Depth 5
    } catch {
        Log "Failed to call $endpoint. Status: $_" "Red"
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Log "Error Response body: $($reader.ReadToEnd())" "Red"
        }
    }
}

# 3. Test All Query/GET Endpoints
Call-Endpoint "/api/hr/dashboard"
Call-Endpoint "/api/hr/employees"
Call-Endpoint "/api/hr/employees/1006"
Call-Endpoint "/api/hr/employees/export/data"
Call-Endpoint "/api/hr/login/1006"
Call-Endpoint "/api/hr/profile/1006"
Call-Endpoint "/api/hr/roles"
Call-Endpoint "/api/hr/employees/1006/roles"
Call-Endpoint "/api/hr/operations"
Call-Endpoint "/api/jobs"
Call-Endpoint "/api/jobs/active"
Call-Endpoint "/api/jobs/health"
Call-Endpoint "/api/hr/workstations"
Call-Endpoint "/api/hr/shifts"
Call-Endpoint "/api/hr/shifts/active"
Call-Endpoint "/api/attendance/today"

Log "============================================"
Log "All endpoint tests completed!" "Magenta"
