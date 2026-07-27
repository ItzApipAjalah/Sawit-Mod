$versions = @("1.20.1", "1.20.2", "1.20.4", "1.20.5", "1.20.6")
$baseDir = "C:\Users\User\Documents"
$repoUrl = "https://github.com/ItzApipAjalah/Sawit-Mod.git"
$gitignorePath = "C:\Users\User\Documents\sawitmod-1.20.1-fabric-like-forge-template\.gitignore"

foreach ($ver in $versions) {
    Write-Host "Processing version $ver..."
    
    # Determine the folder name
    $folderName = "sawitmod-$ver-fabric-like-forge-template"
    if ($ver -eq "1.20.4") { $folderName = "sawitmod-$ver-fabric-like-neoforge-forge-template" }
    if ($ver -eq "1.20.5") { $folderName = "sawitmod-$ver-fabric-neoforge-template" }
    if ($ver -eq "1.20.6") { $folderName = "sawitmod-$ver-fabric-like-neoforge-template" }
    
    $targetDir = Join-Path $baseDir $folderName
    
    if (-not (Test-Path $targetDir)) {
        Write-Host "Directory $targetDir not found, skipping."
        continue
    }

    Set-Location $targetDir

    # Copy gitignore
    if ($ver -ne "1.20.1") {
        Copy-Item -Path $gitignorePath -Destination ".\.gitignore" -Force
    }

    # Ensure git is initialized
    if (-not (Test-Path ".\.git")) {
        git init
    }

    # Ensure remote is added
    $remotes = git remote
    if ($remotes -notcontains "origin") {
        git remote add origin $repoUrl
    } else {
        git remote set-url origin $repoUrl
    }

    # Create/Checkout branch
    # If the branch doesn't exist, checkout -b creates it. If it exists, checkout switches to it.
    git checkout -B $ver

    # Add, Commit, Push
    git add .
    git commit -m "Auto-commit version $ver"
    git push -u origin $ver --force
}

Write-Host "All versions pushed successfully!"
