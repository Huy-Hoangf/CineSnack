param([Parameter(Mandatory = $true)][string]$Server)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$taskSql = Get-Content -LiteralPath (Join-Path $taskRoot 'database/cinesnack.sql') -Raw -Encoding utf8
$taskSql = $taskSql.Replace("`r`n", "`n")
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
function Get-Snapshot {
    return Invoke-Sql @'
SELECT (SELECT
    (SELECT * FROM PHIM ORDER BY phim_id FOR JSON PATH) phim,
    (SELECT * FROM PHONG_CHIEU ORDER BY phong_id FOR JSON PATH) phong,
    (SELECT * FROM GHE ORDER BY ghe_id FOR JSON PATH) ghe,
    (SELECT * FROM SUAT_CHIEU ORDER BY suat_chieu_id FOR JSON PATH) suat,
    (SELECT * FROM COMBO ORDER BY combo_id FOR JSON PATH) combo,
    (SELECT nguoi_dung_id,email,vai_tro,trang_thai FROM NGUOI_DUNG ORDER BY nguoi_dung_id FOR JSON PATH) tai_khoan,
    (SELECT COUNT(*) FROM sys.tables) so_bang,
    (SELECT COUNT(*) FROM sys.indexes) so_index,
    (SELECT COUNT(*) FROM DON_HANG) so_don
    FOR JSON PATH)
'@
}
$taskConnection.Open()
try {
    $taskBefore = Get-Snapshot
    $taskPattern = '(?s)IF OBJECT_ID\(N''VE'',N''U''\).*?(?=\s*THROW 50010,)'
    $taskLegacyProbe = [regex]::Replace($taskSql, $taskPattern, 'IF 1=1', 1)
    if ($taskLegacyProbe -eq $taskSql) { throw 'Không tìm thấy preflight' }
    try { [void](Invoke-Sql $taskLegacyProbe); throw 'Preflight không chặn script' }
    catch [System.Data.SqlClient.SqlException] { if ($_.Exception.Number -ne 50010) { throw } }
    if ((Get-Snapshot) -ne $taskBefore) { throw 'Preflight thay đổi dữ liệu' }
    $taskCommit = "    COMMIT;`nEND TRY"
    $taskCommitAt = $taskSql.LastIndexOf($taskCommit)
    if ($taskCommitAt -lt 0) { throw 'Không tìm thấy commit bao ngoài' }
    $taskFault = $taskSql.Substring(0, $taskCommitAt) + "    THROW 51000, N'Kiểm thử rollback toàn script', 1;`n" + $taskSql.Substring($taskCommitAt)
    try { [void](Invoke-Sql $taskFault); throw 'Fault không chặn script' }
    catch [System.Data.SqlClient.SqlException] { if ($_.Exception.Number -ne 51000) { throw } }
    if ((Get-Snapshot) -ne $taskBefore) { throw 'Lỗi giữa script không rollback toàn bộ' }
    Write-Output 'PASS: preflight dừng trước thay đổi; lỗi sau seed rollback dữ liệu/schema; chỉ dùng CineSnack.'
}
finally { $taskConnection.Dispose() }
