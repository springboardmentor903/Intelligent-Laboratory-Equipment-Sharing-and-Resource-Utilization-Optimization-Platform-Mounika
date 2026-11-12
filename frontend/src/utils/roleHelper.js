/**
 * Authoritative role evaluation helper for the Lab Resource Utilization Platform.
 * Supports role strings with or without 'ROLE_' prefix.
 */
export function normalizeRole(role) {
  if (!role) return '';
  const r = String(role).trim().toUpperCase();
  return r.startsWith('ROLE_') ? r.substring(5) : r;
}

export function isSystemAdmin(user) {
  return normalizeRole(user?.role) === 'SYSTEM_ADMIN';
}

export function isInstitutionAdmin(user) {
  return normalizeRole(user?.role) === 'INSTITUTION_ADMIN';
}

export function isLabManager(user) {
  return normalizeRole(user?.role) === 'LAB_MANAGER';
}

export function isDepartmentHead(user) {
  return normalizeRole(user?.role) === 'DEPARTMENT_HEAD';
}

export function isLabTechnician(user) {
  return normalizeRole(user?.role) === 'LAB_TECHNICIAN';
}

export function isResearcherStudent(user) {
  const r = normalizeRole(user?.role);
  return r === 'RESEARCHER_STUDENT' || r === 'RESEARCHER' || r === 'STUDENT';
}

/**
 * Roles permitted to make bookings: Researcher / Student, Department Head, and System Admin.
 */
export function canBookEquipment(user) {
  return isResearcherStudent(user) || isDepartmentHead(user) || isSystemAdmin(user);
}

/**
 * Roles permitted to approve bookings: Lab Manager (primary), Institution Admin (backup), System Admin.
 */
export function canApproveBookings(user) {
  return isLabManager(user) || isInstitutionAdmin(user) || isSystemAdmin(user);
}

/**
 * Roles permitted to access utilization analytics dashboards:
 * Department Head (own dept), Lab Manager (own inst), Institution Admin (own inst), System Admin (all).
 */
export function canViewUtilizationAnalytics(user) {
  return isDepartmentHead(user) || isLabManager(user) || isInstitutionAdmin(user) || isSystemAdmin(user);
}

/**
 * Roles permitted to manage sharing listings and partnership agreements:
 * Institution Administrator and System Administrator.
 */
export function canManageSharing(user) {
  return isInstitutionAdmin(user) || isSystemAdmin(user);
}

/**
 * Roles permitted to register and edit equipment inventory:
 * Lab Manager, Lab Technician, Department Head, Institution Admin, System Admin.
 */
export function canManageEquipment(user) {
  const r = normalizeRole(user?.role);
  return ['LAB_MANAGER', 'LAB_TECHNICIAN', 'DEPARTMENT_HEAD', 'INSTITUTION_ADMIN', 'SYSTEM_ADMIN'].includes(r);
}

/**
 * Roles permitted to submit inter-institution access requests:
 * Researchers/Students and Department Heads who belong to an institution.
 */
export function canRequestSharedAccess(user) {
  return canBookEquipment(user) && Boolean(user?.institution?.id || user?.institutionId);
}
