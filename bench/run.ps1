param(
  [Parameter(Mandatory = $true)]
  [ValidateSet('mono', 'micro')]
  [string]$Label,
  [string]$Base = 'http://127.0.0.1:8081',
  [int]$Vus = 50,
  [int]$Duration = 30,
  [string]$Scenario = 'all'
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
node .\run.mjs --label $Label --base $Base --vus $Vus --duration $Duration --scenario $Scenario @args
