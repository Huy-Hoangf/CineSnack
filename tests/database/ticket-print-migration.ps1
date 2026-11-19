param(
    [Parameter(Mandatory = $true)]
    [string]$Server
)

$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$taskSchema = Get-Content -LiteralPath (Join-Path $taskRoot 'database/cinesnack.sql') -Raw -Encoding utf8
$taskStart = $taskSchema.IndexOf("IF COL_LENGTH(''VE'', ''in_ve_luc'')")
$taskEnd = $taskSchema.IndexOf("EXEC(N''UPDATE h", $taskStart)
if ($taskStart -lt 0 -or $taskEnd -lt 0) {
    throw 'Không tìm thấy phần nâng cấp in vé.'
}

# Chạy đúng đoạn nâng cấp trên hai bảng riêng trong tempdb, rồi rollback toàn bộ.
$taskSuffix = [Guid]::NewGuid().ToString('N')
$taskTicketTable = "ReviewVe_$taskSuffix"
$taskStaffTable = "ReviewNhanVien_$taskSuffix"
$taskForeignKey = "FK_ReviewInVe_$taskSuffix"
$taskCheck = "CK_ReviewInVe_$taskSuffix"
$taskMigration = $taskSchema.Substring($taskStart, $taskEnd - $taskStart).Replace("''", "'")
$taskMigration = $taskMigration.Replace("'VE'", "'$taskTicketTable'")
$taskMigration = $taskMigration.Replace('ALTER TABLE VE', "ALTER TABLE dbo.$taskTicketTable")
$taskMigration = $taskMigration.Replace('REFERENCES NGUOI_DUNG', "REFERENCES dbo.$taskStaffTable")
$taskMigration = $taskMigration.Replace('FK_VE_NHAN_VIEN_IN', $taskForeignKey).Replace('CK_VE_IN', $taskCheck)
$taskConnection = [System.Data.SqlClient.SqlConnection]::new("Server=$Server;Database=tempdb;Integrated Security=True;TrustServerCertificate=True;Connection Timeout=5")
function Invoke-TestSql([string]$Sql) {
    $taskCommand = $taskConnection.CreateCommand()
    try {
        $taskCommand.CommandText = $Sql
        return $taskCommand.ExecuteScalar()
    }
    finally {
        $taskCommand.Dispose()
    }
}

function Assert-SqlRejected([string]$Sql, [string]$Constraint) {
    $taskRejected = $false
    try {
        [void](Invoke-TestSql $Sql)
    }
    catch {
        if ($_.Exception.ToString().Contains($Constraint)) {
            $taskRejected = $true
        }
        else {
            throw
        }
    }
    if (!$taskRejected) {
        throw "Ràng buộc $Constraint không chặn dữ liệu sai."
    }
}

$taskConnection.Open()
try {
    [void](Invoke-TestSql "BEGIN TRANSACTION; CREATE TABLE dbo.$taskStaffTable(nguoi_dung_id BIGINT PRIMARY KEY); CREATE TABLE dbo.$taskTicketTable(ve_id BIGINT);")
    [void](Invoke-TestSql $taskMigration)
    [void](Invoke-TestSql $taskMigration)
    if ((Invoke-TestSql "SELECT COUNT(*) FROM sys.columns WHERE object_id=OBJECT_ID('dbo.$taskTicketTable')") -ne 3) {
        throw 'Thiếu cột in vé sau khi nâng cấp.'
    }
    Assert-SqlRejected "INSERT INTO dbo.$taskTicketTable VALUES(1,SYSDATETIME(),NULL);" $taskCheck
    Assert-SqlRejected "INSERT INTO dbo.$taskTicketTable VALUES(2,SYSDATETIME(),99);" $taskForeignKey
    [void](Invoke-TestSql "INSERT INTO dbo.$taskStaffTable VALUES(7); INSERT INTO dbo.$taskTicketTable VALUES(3,SYSDATETIME(),7);")
    Write-Output 'PASS: nâng cấp bảng cũ, chạy lại an toàn, CHECK và khóa ngoại lượt in hợp lệ.'
}
finally {
    try {
        [void](Invoke-TestSql 'IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;')
    }
    finally {
        $taskConnection.Dispose()
    }
}
