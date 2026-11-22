#Requires -Version 5.1
<#
.SYNOPSIS
  Bump Kiln version, write CHANGELOG, optionally commit/tag/push for GitHub Release CI.

.DESCRIPTION
  Default bump is patch (0.2.5 -> 0.2.6). versionCode always +1.
  Without -Yes: prints the planned version and changelog section, writes nothing.
  With -Yes: updates files, commits, creates annotated tag vX.Y.Z, pushes branch + tag
  (triggers .github/workflows/release.yml). About reads versionName from the APK.

.PARAMETER Bump
  patch (default), minor, or major.

.PARAMETER Yes
  Commit, tag, and push. Required to mutate the repo.
#>
[CmdletBinding()]
param(
    [ValidateSet("patch", "minor", "major")]
    [string] $Bump = "patch",
    [switch] $Yes
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $here = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
    return (Resolve-Path (Join-Path $here "..")).Path
}

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $GitArgs)
    & git @GitArgs
    if ($LASTEXITCODE -ne 0) {
        throw "git $($GitArgs -join ' ') failed with exit $LASTEXITCODE"
    }
}

function Read-KilnVersion {
    param([string] $KilnAndroidPath)
    $text = Get-Content -Raw -Path $KilnAndroidPath
    if ($text -notmatch 'private const val VERSION_CODE = (\d+)') {
        throw "VERSION_CODE not found in $KilnAndroidPath"
    }
    $code = [int]$Matches[1]
    if ($text -notmatch 'versionName = "(\d+)\.(\d+)\.(\d+)"') {
        throw "versionName not found in $KilnAndroidPath"
    }
    return [pscustomobject]@{
        Code  = $code
        Major = [int]$Matches[1]
        Minor = [int]$Matches[2]
        Patch = [int]$Matches[3]
        Name  = "$($Matches[1]).$($Matches[2]).$($Matches[3])"
    }
}

function Get-BumpedVersion {
    param($Current, [string] $Kind)
    $major = $Current.Major
    $minor = $Current.Minor
    $patch = $Current.Patch
    switch ($Kind) {
        "major" { $major++; $minor = 0; $patch = 0 }
        "minor" { $minor++; $patch = 0 }
        default { $patch++ }
    }
    return [pscustomobject]@{
        Code  = $Current.Code + 1
        Major = $major
        Minor = $minor
        Patch = $patch
        Name  = "$major.$minor.$patch"
    }
}

function Get-UnreleasedBody {
    param([string] $ChangelogPath)
    $lines = Get-Content -Path $ChangelogPath
    $body = [System.Collections.Generic.List[string]]::new()
    $inUnreleased = $false
    foreach ($line in $lines) {
        if ($line -match '^## \[Unreleased\]') {
            $inUnreleased = $true
            continue
        }
        if ($inUnreleased -and $line -match '^## \[') {
            break
        }
        if ($inUnreleased) {
            $body.Add($line)
        }
    }
    while ($body.Count -gt 0 -and [string]::IsNullOrWhiteSpace($body[0])) {
        $body.RemoveAt(0)
    }
    while ($body.Count -gt 0 -and [string]::IsNullOrWhiteSpace($body[$body.Count - 1])) {
        $body.RemoveAt($body.Count - 1)
    }
    return $body
}

function Get-CommitSections {
    param([string] $SinceTag)
    $range = if ($SinceTag) { "$SinceTag..HEAD" } else { "HEAD" }
    $raw = & git log $range --pretty=format:"%s" 2>$null
    if (-not $raw) { return @() }
    $added = [System.Collections.Generic.List[string]]::new()
    $fixed = [System.Collections.Generic.List[string]]::new()
    $changed = [System.Collections.Generic.List[string]]::new()
    foreach ($subject in ($raw -split "`n")) {
        $s = $subject.Trim()
        if ([string]::IsNullOrWhiteSpace($s)) { continue }
        if ($s -match '^(docs|test|chore|ci|build|style)(\(.+\))?(!)?:') { continue }
        if ($s -match '^feat(\(.+\))?(!)?:\s*(.+)$') {
            $added.Add("- $($Matches[3].Trim())")
        } elseif ($s -match '^fix(\(.+\))?(!)?:\s*(.+)$') {
            $fixed.Add("- $($Matches[3].Trim())")
        } elseif ($s -match '^(refactor|perf)(\(.+\))?(!)?:\s*(.+)$') {
            $changed.Add("- $($Matches[4].Trim())")
        }
    }
    $out = [System.Collections.Generic.List[string]]::new()
    if ($added.Count -gt 0) {
        $out.Add("### Added"); $out.Add("")
        $added | ForEach-Object { $out.Add($_) }
        $out.Add("")
    }
    if ($fixed.Count -gt 0) {
        $out.Add("### Fixed"); $out.Add("")
        $fixed | ForEach-Object { $out.Add($_) }
        $out.Add("")
    }
    if ($changed.Count -gt 0) {
        $out.Add("### Changed"); $out.Add("")
        $changed | ForEach-Object { $out.Add($_) }
        $out.Add("")
    }
    return $out
}

function Build-ChangelogSection {
    param(
        [string] $NewName,
        [string] $Date,
        [int] $NewCode,
        $UnreleasedBody,
        $CommitSections
    )
    $sb = [System.Text.StringBuilder]::new()
    [void]$sb.AppendLine("## [$NewName] — $Date")
    [void]$sb.AppendLine()
    $hasBody = $false
    foreach ($line in $UnreleasedBody) {
        [void]$sb.AppendLine($line)
        $hasBody = $true
    }
    if ($hasBody) { [void]$sb.AppendLine() }
    foreach ($line in $CommitSections) {
        [void]$sb.AppendLine($line)
        $hasBody = $true
    }
    $text = $sb.ToString()
    if ($text -notmatch '(?m)^### Changed\s*$') {
        [void]$sb.AppendLine("### Changed")
        [void]$sb.AppendLine()
    }
    [void]$sb.AppendLine("- Version **$NewName** (versionCode $NewCode)")
    [void]$sb.AppendLine()
    return $sb.ToString().TrimEnd() + "`n`n"
}

$root = Get-RepoRoot
Set-Location $root

$kilnAndroid = Join-Path $root "build-logic\convention\src\main\kotlin\dev\involvex\kiln\convention\KilnAndroid.kt"
$changelogPath = Join-Path $root "CHANGELOG.md"
$readmePath = Join-Path $root "README.md"
$roadmapPath = Join-Path $root "docs\ROADMAP.md"

$status = (& git status --porcelain)
if ($status) {
    throw "Working tree is dirty. Commit or stash before releasing.`n$status"
}

$branch = (& git rev-parse --abbrev-ref HEAD).Trim()
if ($branch -ne "main") {
    throw "Must run on main (current branch: $branch)."
}

$current = Read-KilnVersion -KilnAndroidPath $kilnAndroid
$latestTag = (& git tag -l "v*" --sort=-v:refname | Select-Object -First 1)
if ($latestTag) {
    $latestTag = $latestTag.Trim()
    $tagVersion = $latestTag.TrimStart("v")
    if ($tagVersion -ne $current.Name) {
        throw "versionName $($current.Name) does not match latest tag $latestTag. Align KilnAndroid.kt with the last release first."
    }
} else {
    Write-Warning "No v* tags found; continuing from versionName $($current.Name)."
}

$next = Get-BumpedVersion -Current $current -Kind $Bump
$date = Get-Date -Format "yyyy-MM-dd"
$newTag = "v$($next.Name)"

$unreleased = Get-UnreleasedBody -ChangelogPath $changelogPath
$commitSections = Get-CommitSections -SinceTag $latestTag
$section = Build-ChangelogSection `
    -NewName $next.Name `
    -Date $date `
    -NewCode $next.Code `
    -UnreleasedBody $unreleased `
    -CommitSections $commitSections

Write-Host "Release plan:"
Write-Host "  bump:        $Bump"
Write-Host "  versionName: $($current.Name) -> $($next.Name)"
Write-Host "  versionCode: $($current.Code) -> $($next.Code)"
Write-Host "  tag:         $newTag"
Write-Host ""
Write-Host "Draft CHANGELOG section:"
Write-Host $section

if (-not $Yes) {
    Write-Host "Dry run only (no files changed). Re-run with -Yes to commit, tag, and push."
    exit 0
}

$androidText = Get-Content -Raw -Path $kilnAndroid
$androidText = $androidText -replace "private const val VERSION_CODE = $($current.Code)", "private const val VERSION_CODE = $($next.Code)"
$androidText = $androidText -replace "versionName = `"$($current.Name)`"", "versionName = `"$($next.Name)`""
Set-Content -Path $kilnAndroid -Value $androidText -NoNewline

$cl = Get-Content -Raw -Path $changelogPath
if ($cl -notmatch '(?m)^## \[Unreleased\]\s*\r?\n') {
    throw "CHANGELOG.md missing ## [Unreleased] heading"
}
$cl = [regex]::Replace(
    $cl,
    '(?ms)^## \[Unreleased\]\s*\r?\n.*?(?=^## \[)',
    "## [Unreleased]`n`n$section",
    1
)
Set-Content -Path $changelogPath -Value $cl -NoNewline

$readme = Get-Content -Raw -Path $readmePath
$readme = $readme -replace "``$([regex]::Escape($current.Name))`` \(versionCode $($current.Code)\)", "``$($next.Name)`` (versionCode $($next.Code))"
$readme = $readme -replace "(?m)^(- \*\*Status:\*\* ``)$([regex]::Escape($current.Name))(``)", "`${1}$($next.Name)`${2}"
$readme = $readme -replace "(?m)^(- \*\*Version:\*\* ``)$([regex]::Escape($current.Name))(``)", "`${1}$($next.Name)`${2}"
Set-Content -Path $readmePath -Value $readme -NoNewline

$roadmap = Get-Content -Raw -Path $roadmapPath
$roadmap = $roadmap -replace "App version \*\*$([regex]::Escape($current.Name))\*\* \(code $($current.Code)\)", "App version **$($next.Name)** (code $($next.Code))"
$roadmap = $roadmap -replace "tag ``v$([regex]::Escape($current.Name))``", "tag ``$newTag``"
$roadmap = $roadmap -replace "``kiln-$([regex]::Escape($current.Name))\.apk``", "``kiln-$($next.Name).apk``"
Set-Content -Path $roadmapPath -Value $roadmap -NoNewline

Invoke-Git add -- $kilnAndroid $changelogPath $readmePath $roadmapPath
Invoke-Git commit -m "chore: release $($next.Name)"
Invoke-Git tag -a $newTag -m "Kiln $($next.Name)"
Invoke-Git push origin main
Invoke-Git push origin $newTag

Write-Host "Released $($next.Name). GitHub Actions Release workflow should build kiln-$($next.Name).apk."
