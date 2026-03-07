$countries = Get-Content "$PSScriptRoot\countries_ingest.json" -Raw | ConvertFrom-Json
$total = $countries.Count
$ok = 0
$fail = 0

Write-Host "Ingesting $total countries..." -ForegroundColor Cyan

for ($i = 0; $i -lt $total; $i++) {
    $country = $countries[$i]
    $num = $i + 1
    $body = $country | ConvertTo-Json -Depth 5

    try {
        $result = Invoke-RestMethod -Uri "http://localhost:8080/api/documents/ingest" -Method POST -ContentType "application/json" -Body $body -TimeoutSec 30
        $ok++
        Write-Host "[$num/$total] OK  $($country.title) - $($result.chunksCreated) chunk(s)" -ForegroundColor Green
    } catch {
        $fail++
        $msg = $_.Exception.Message
        Write-Host "[$num/$total] FAIL $($country.title) - $msg" -ForegroundColor Red
    }

    Start-Sleep -Milliseconds 100
}

Write-Host ""
Write-Host "Done! $ok OK, $fail failed." -ForegroundColor Cyan
Write-Host "Check: http://localhost:8080/api/diagnostics" -ForegroundColor Yellow
