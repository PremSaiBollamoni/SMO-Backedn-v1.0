-- Update roles with exact activities from frontend screen guards

-- HR: Dashboard, Role Management, Employee Management, Attendance Reports, Shifts & Breaks
UPDATE role SET activities = 'HR_DASHBOARD,HR_MANAGE_ROLES,HR_MANAGE_EMPLOYEES,HR_ATTENDANCE_REPORT'
WHERE role_name = 'HR';

-- SUPERVISOR: Work Assignment, Efficiency, Assignment History, Attendance, Line Balancing
UPDATE role SET activities = 'SUPERVISOR_WORK_ASSIGNMENT,SUPERVISOR_EFFICIENCY,SUPERVISOR_HISTORY,SUPERVISOR_ATTENDANCE,SUPERVISOR_LINE_BALANCING'
WHERE role_name = 'SUPERVISOR';

-- GM: Production, Reports, Efficiency
UPDATE role SET activities = 'GM_VIEW_PRODUCTION,GM_VIEW_REPORTS,GM_VIEW_EFFICIENCY'
WHERE role_name = 'GM';
