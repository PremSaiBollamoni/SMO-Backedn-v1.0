# Configure git user email for tracking
git config user.email "premsai200804@gmail.com"

# Backend - Attendance Service (QR freeing logic)
git add "src/main/java/com/cutm/smo/services/AttendanceService.java"
git commit -m "feat: auto-checkout employees on free-all-qrs endpoint

- Free all QRs now auto-checks out all active employees (status=CHECKED_IN)
- Auto-checkout sets employee status to CHECKED_OUT with current timestamp
- Allows employee to re-check-in with same QR code after checkout
- Transactional operation ensures data consistency"

# Backend - Attendance Repository (findByAttDateAndStatus)
git add "src/main/java/com/cutm/smo/repositories/AttendanceRepository.java"
git commit -m "feat: add findByAttDateAndStatus repository method

- Query method to find attendance records by date and status
- Used for auto-checkout in free-all-qrs flow
- Supports finding all CHECKED_IN employees for a given date"

# Backend - TempQrMapping Repository (findByQrTokenAndMappingDate)
git add "src/main/java/com/cutm/smo/repositories/TempQrMappingRepository.java"
git commit -m "feat: add findByQrTokenAndMappingDate method without freed status filter

- Find QR mapping regardless of freed status
- Used to mark mapping as freed after employee checkout
- Complements existing findByQrTokenAndMappingDateAndFreedFalse"

# Backend - README (comprehensive update)
git add "README.md"
git commit -m "docs: update backend README with API and architecture details

- Add PALMS project name and technical badges
- Document 40+ completed backend features
- List 15+ coming soon features
- Include 461 total commits
- Full database schema documentation (operation, workstation, job_assignment, tray, etc.)
- Complete API endpoints listing
- Architecture diagram and project structure
- QR workflow explanation
- Excel import design details
- Key concepts: null safety, SAM calculation
- Setup and configuration instructions
- Project tracking email: premsai200804@gmail.com"

# Push to remote
Write-Host "Pushing backend commits to remote repository..." -ForegroundColor Cyan
git push -u origin main

Write-Host "All backend commits pushed successfully!" -ForegroundColor Green
Write-Host "Tracked by: premsai200804@gmail.com" -ForegroundColor Yellow
Write-Host ""
git log --oneline -10
