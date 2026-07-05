#Requires -Version 5.1
<#
.SYNOPSIS
    MariaDB 개발용 컨테이너를 시작하거나 중지합니다.
.DESCRIPTION
    docker-compose.dev.yml 을 사용해 MariaDB만 실행합니다.
    Spring Boot를 MariaDB로 연결하려면 .env.local 의 MariaDB 섹션 주석을 해제하세요.
.PARAMETER Down
    컨테이너를 중지합니다.
.PARAMETER Volumes
    -Down 과 함께 사용 시 볼륨까지 삭제합니다 (데이터 초기화).
.EXAMPLE
    .\scripts\dev-db.ps1
    .\scripts\dev-db.ps1 -Down
    .\scripts\dev-db.ps1 -Down -Volumes
#>
param(
    [switch]$Down,
    [switch]$Volumes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

if ($Down) {
    $args = @('compose', '-f', 'docker-compose.dev.yml', 'down')
    if ($Volumes) { $args += '-v' }
    Write-Host "[dev-db] MariaDB 컨테이너 중지..."
    & docker @args
    exit $LASTEXITCODE
}

Write-Host "[dev-db] MariaDB 개발 컨테이너를 시작합니다..."
Write-Host "  host: localhost:3306"
Write-Host "  user: kraft_lotto / devpass"
Write-Host "  db:   kraft_lotto"
Write-Host ""
Write-Host "  .env.local 에서 MariaDB 섹션 주석을 해제해야 백엔드가 연결됩니다."
Write-Host ""
& docker compose -f docker-compose.dev.yml up -d

if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host ""
Write-Host "[dev-db] 헬스체크 대기 중..."
$timeout = 60
$elapsed = 0
while ($elapsed -lt $timeout) {
    $status = & docker inspect --format='{{.State.Health.Status}}' kraft-mariadb-dev 2>$null
    if ($status -eq 'healthy') {
        Write-Host "[dev-db] MariaDB 준비 완료."
        exit 0
    }
    Start-Sleep -Seconds 3
    $elapsed += 3
    Write-Host "  대기 중... ($elapsed s)"
}
Write-Host "[dev-db] 경고: $timeout 초 내에 healthy 상태가 되지 않았습니다."
exit 1
