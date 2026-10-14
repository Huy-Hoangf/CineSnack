param([Parameter(Mandatory = $true)][string]$Server)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$taskConnection = [System.Data.SqlClient.SqlConnection]::new("Server=$Server;Database=CineSnack;Integrated Security=True;TrustServerCertificate=True;Connection Timeout=5")

function Invoke-Sql([string]$Sql) {
    $taskCommand = $taskConnection.CreateCommand()
    try {
        $taskCommand.CommandTimeout = 30
        $taskCommand.CommandText = $Sql
        return $taskCommand.ExecuteScalar()
    }
    finally { $taskCommand.Dispose() }
}

function Invoke-File([string]$Name) {
    $taskText = Get-Content -LiteralPath (Join-Path $taskRoot "database/$Name") -Raw -Encoding utf8
    foreach ($taskBatch in [regex]::Split($taskText, '(?im)^\s*GO\s*\r?$')) {
        if (![string]::IsNullOrWhiteSpace($taskBatch)) { [void](Invoke-Sql $taskBatch) }
    }
}

function Get-Snapshot {
    return Invoke-Sql @'
SELECT (
    SELECT (SELECT * FROM PHIM ORDER BY phim_id FOR JSON PATH) phim,
           (SELECT * FROM COMBO ORDER BY combo_id FOR JSON PATH) combo,
           (SELECT * FROM PHONG_CHIEU ORDER BY phong_id FOR JSON PATH) phong,
           (SELECT * FROM GHE ORDER BY ghe_id FOR JSON PATH) ghe,
           (SELECT * FROM SUAT_CHIEU ORDER BY suat_chieu_id FOR JSON PATH) suat,
           (SELECT COUNT(*) FROM NGUOI_DUNG) nguoi_dung,
           (SELECT COUNT(*) FROM DON_HANG) don_hang,
           (SELECT COUNT(*) FROM THANH_TOAN) thanh_toan,
           (SELECT COUNT(*) FROM VE) ve,
           (SELECT COUNT(*) FROM HOA_DON_VE) hoa_don_ve,
           (SELECT COUNT(*) FROM HOA_DON_BONG_NUOC) hoa_don_bong_nuoc
    FOR JSON PATH
)
'@
}

$taskConnection.Open()
try {
    $taskBefore = Get-Snapshot
    foreach ($taskPass in 1..2) {
        Invoke-File 'cinesnack.sql'
        if ((Get-Snapshot) -ne $taskBefore) { throw "Dữ liệu thay đổi ở lượt chạy $taskPass" }
    }
    [void](Invoke-Sql "BEGIN TRANSACTION; UPDATE COMBO SET gia_ban=123456 WHERE ten_combo=N'Combo Một Mình Vui';")
    try {
        Invoke-File 'cinesnack.sql'
        if ((Invoke-Sql "SELECT gia_ban FROM COMBO WHERE ten_combo=N'Combo Một Mình Vui'") -ne 123456) {
            throw 'Seed ghi đè giá combo đã chỉnh'
        }
    }
    finally { [void](Invoke-Sql 'IF @@TRANCOUNT>0 ROLLBACK;') }
    if ((Get-Snapshot) -ne $taskBefore) { throw 'Dữ liệu không được khôi phục sau kiểm thử giá' }
    Write-Output 'PASS: chạy lại không mất/trùng dữ liệu; seed giữ giá đã chỉnh; thử giá đã rollback.'
}
finally { $taskConnection.Dispose() }
